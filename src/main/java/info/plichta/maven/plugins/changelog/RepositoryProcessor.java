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

import info.plichta.maven.plugins.changelog.handlers.CommitHandler;
import info.plichta.maven.plugins.changelog.model.CommitWrapper;
import info.plichta.maven.plugins.changelog.model.TagWrapper;
import org.apache.maven.plugin.logging.Log;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class RepositoryProcessor {
    private final Pattern tagPattern;

    private final Log log;

    private final boolean deduplicateChildCommits;
    private final String toRef;
    private final String nextRelease;
    private final String gitHubUrl;
    private final Predicate<RevCommit> commitFilter;
    private final List<CommitHandler> commitHandlers = new ArrayList<>();
    private final TreeFilter pathFilter;

    public RepositoryProcessor(boolean deduplicateChildCommits, String toRef, String nextRelease, String gitHubUrl,
                               Predicate<RevCommit> commitFilter, List<CommitHandler> commitHandlers, String pathFilter,
                               String tagPrefix, Log log) {
        this.deduplicateChildCommits = deduplicateChildCommits;
        this.toRef = toRef;
        this.nextRelease = nextRelease;
        this.gitHubUrl = gitHubUrl;
        this.commitFilter = commitFilter;
        if (!isBlank(pathFilter) && !"/".equals(pathFilter)) {
            this.pathFilter = PathFilter.create(pathFilter);
        } else {
            this.pathFilter = PathFilter.ALL;
        }
        this.commitHandlers.addAll(commitHandlers);
        tagPattern = Pattern.compile(tagPrefix + "-([^-]+?)$");
        this.log = log;
    }

    public List<TagWrapper> process(File repoRoot) throws IOException {
        try (Repository repository = new RepositoryBuilder().findGitDir(repoRoot).build()) {
            return process(repository);
        }
    }

    public List<TagWrapper> process(Repository repository) throws IOException {
        final List<TagWrapper> tags = new ArrayList<>();
        log.info("Processing git repository " + repository.getDirectory());

        final ObjectId head = repository.resolve(toRef);
        if (head == null) {
            return tags;
        }
        try (RevWalk walk = new RevWalk(repository)) {
            walk.sort(RevSort.TOPO);
            final Map<ObjectId, TagWrapper> tagMapping = extractTags(repository, walk);

            TagWrapper currentTag = new TagWrapper(nextRelease);
            tags.add(currentTag);

            RevCommit commit = walk.parseCommit(head);
            while (commit != null) {
                currentTag = tagMapping.getOrDefault(commit.getId(), currentTag);
                if (tagMapping.containsKey(commit.getId())) {
                    tags.add(currentTag);
                }
                final CommitWrapper commitWrapper = processCommit(commit);

                if (commitFilter.test(commit) && isInPath(repository, walk, commit)) {
                    currentTag.getCommits().add(commitWrapper);
                }
                final RevCommit[] parents = commit.getParents();
                if (parents != null && parents.length > 0) {
                    final RevCommit parent = walk.parseCommit(parents[0]);

                    try (RevWalk childWalk = new RevWalk(repository)) {
                        childWalk.markStart(childWalk.parseCommit(commit));
                        childWalk.markUninteresting(childWalk.parseCommit(parent));
                        childWalk.next();
                        for (RevCommit childCommit : childWalk) {
                            final CommitWrapper childWrapper = processCommit(childCommit);
                            if (commitFilter.test(childCommit) && isInPath(repository, walk, commit) && !(deduplicateChildCommits && Objects.equals(commitWrapper.getTitle(), childWrapper.getTitle()))) {
                                commitWrapper.getChildren().add(childWrapper);
                            }
                        }

                    }
                    commit = parent;
                } else {
                    commit = null;
                }
            }
        }

        return tags;
    }

    private Map<ObjectId, TagWrapper> extractTags(Repository repository, RevWalk walk) throws IOException {
        final Map<ObjectId, TagWrapper> tagMapping = new HashMap<>();
        for (Entry<String, Ref> entry : repository.getTags().entrySet()) {
            String name = entry.getKey();

            final Matcher matcher = tagPattern.matcher(name);
            if (matcher.matches()) {
                name = matcher.group(1);
                tagMapping.put(walk.parseCommit(entry.getValue().getObjectId()), new TagWrapper(name));
            }
        }
        return tagMapping;
    }

    private boolean isInPath(Repository repository, RevWalk walk, RevCommit commit) throws IOException {
        if (commit.getParentCount() == 0) {
            RevTree tree = commit.getTree();
            try (TreeWalk treeWalk = new TreeWalk(repository)) {
                treeWalk.addTree(tree);
                treeWalk.setRecursive(true);
                treeWalk.setFilter(pathFilter);
                return treeWalk.next();
            }
        } else {
            DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);
            df.setRepository(repository);
            df.setPathFilter(pathFilter);
            RevCommit parent = walk.parseCommit(commit.getParent(0).getId());
            List<DiffEntry> diffs = df.scan(parent.getTree(), commit.getTree());
            return !diffs.isEmpty();
        }
    }

    private CommitWrapper processCommit(RevCommit commit) {
        final CommitWrapper commitWrapper = new CommitWrapper(commit, gitHubUrl);
        for (CommitHandler listener : commitHandlers) {
            listener.handle(commitWrapper);
        }
        return commitWrapper;
    }
}
