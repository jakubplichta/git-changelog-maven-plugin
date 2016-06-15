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

package info.plichta.maven.plugins.changelog.handlers;

import info.plichta.maven.plugins.changelog.handlers.JiraHandler.JiraIssue;
import info.plichta.maven.plugins.changelog.handlers.JiraHandler.JiraLink;
import info.plichta.maven.plugins.changelog.handlers.JiraHandler.TitleToken;
import info.plichta.maven.plugins.changelog.model.CommitWrapper;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.junit.RepositoryTestCase;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Test;

import static com.shazam.shazamcrest.matcher.Matchers.sameBeanAs;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.hamcrest.collection.IsMapContaining.hasValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class JiraHandlerTest extends RepositoryTestCase {

    private static final String SERVER = "server";
    private JiraHandler handler;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        handler = new JiraHandler(SERVER);
    }

    @Test
    public void test() throws GitAPIException {
        try (Git git = new Git(db)) {
            final RevCommit commit = git.commit().setMessage("This is commit with JIRA-1234 issue").call();
            final CommitWrapper wrapper = new CommitWrapper(commit, SERVER);
            handler.handle(wrapper);
            final JiraIssue issue = new JiraIssue();
            issue.getTitle().add(new TitleToken("This is commit with ", null));
            issue.getTitle().add(new TitleToken("JIRA-1234", new JiraLink("JIRA-1234", SERVER + "/browse/JIRA-1234")));
            issue.getTitle().add(new TitleToken(" issue", null));

            assertThat(wrapper.getTitle(), is("This is commit with JIRA-1234 issue"));
            assertThat(wrapper.getExtensions(), hasKey("jira"));
            assertThat(wrapper.getExtensions(), hasValue(sameBeanAs(issue)));
        }
    }

    @Test
    public void testNoJira() throws GitAPIException {
        try (Git git = new Git(db)) {
            final RevCommit commit = git.commit().setMessage("Ordinary commit").call();
            final CommitWrapper wrapper = new CommitWrapper(commit, SERVER);
            handler.handle(wrapper);
            assertThat(wrapper.getTitle(), is("Ordinary commit"));
            assertThat(wrapper.getExtensions(), not(hasKey("jira")));
        }
    }

}