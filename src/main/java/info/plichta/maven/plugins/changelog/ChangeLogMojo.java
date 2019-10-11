/*
 * Copyright 2016 git-changelog-maven-plugin contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package info.plichta.maven.plugins.changelog;

import static org.apache.commons.lang3.StringUtils.stripEnd;

import info.plichta.maven.plugins.changelog.handlers.CommitHandler;
import info.plichta.maven.plugins.changelog.handlers.JiraHandler;
import info.plichta.maven.plugins.changelog.handlers.PullRequestHandler;
import info.plichta.maven.plugins.changelog.model.ChangeLog;
import info.plichta.maven.plugins.changelog.model.TagWrapper;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mojo(name = "git-changelog")
public class ChangeLogMojo extends AbstractMojo {

    private static final String DEFAULT_TEMPLATE = "changelog.mustache";

    @Parameter(property = "project.basedir")
    private File repoRoot;

    @Parameter(defaultValue = "${project.basedir}/CHANGELOG.md")
    private File outputFile;

    @Parameter(defaultValue = "Change Log")
    private String reportTitle;

    @Parameter(defaultValue = "${project.basedir}/" + DEFAULT_TEMPLATE)
    private File templateFile;

    @Parameter(defaultValue = ".*")
    private String includeCommits;

    @Parameter(defaultValue = "^\\[maven-release-plugin\\].*")
    private String excludeCommits;

    @Parameter
    private String gitHubUrl;

    @Parameter
    private String scmUrl;

    @Parameter(property = "project.version")
    private String nextRelease;

    @Parameter(defaultValue = "true")
    private boolean deduplicateChildCommits;

    @Parameter
    private String jiraServer;

    @Parameter(defaultValue = "HEAD")
    private String toRef;

    @Parameter(defaultValue = "")
    private String pathFilter;

    @Parameter(property = "project.artifactId")
    private String tagPrefix;

    @Parameter
    private LocalDateTime ignoreOlderThen;

    @Parameter(defaultValue = "false")
    private boolean skip;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            return;     // execution skipped
        }

        final String template = Optional.of(templateFile)
                .filter(File::canRead)
                .map(File::toString)
                .orElse(DEFAULT_TEMPLATE);

        final ChangeLogWriter logGenerator = new ChangeLogWriter(template, getLog());
        final CommitFilter commitFilter = new CommitFilter(includeCommits, excludeCommits, ignoreOlderThen);

        final List<CommitHandler> commitHandlers = new ArrayList<>();
        if (gitHubUrl != null || scmUrl != null) {
            commitHandlers.add(new PullRequestHandler(gitHubUrl));
        }
        if (jiraServer != null) {
            commitHandlers.add(new JiraHandler(jiraServer));
        }
        final RepositoryProcessor repositoryProcessor = new RepositoryProcessor(deduplicateChildCommits, toRef, nextRelease,
                constructScmUrl(),
                commitFilter, commitHandlers, pathFilter, tagPrefix, getLog());

        final List<TagWrapper> tags;
        try {
            tags = repositoryProcessor.process(repoRoot);
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot process repository " + repoRoot, e);
        }
        logGenerator.write(outputFile, new ChangeLog(reportTitle, tags));
    }

    private String constructScmUrl() {
        if (gitHubUrl != null) {
            return stripEnd(gitHubUrl, "/") + "/commit";
        }
        if (scmUrl != null) {
            return stripEnd(scmUrl, "/");
        }
        return null;
    }

}
