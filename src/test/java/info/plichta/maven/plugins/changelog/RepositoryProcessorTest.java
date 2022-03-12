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

import info.plichta.maven.plugins.changelog.model.CommitWrapper;
import info.plichta.maven.plugins.changelog.model.TagWrapper;
import org.apache.maven.plugin.logging.Log;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeCommand.FastForwardMode;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.junit.RepositoryTestCase;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.ImmutableMap.of;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;

public class RepositoryProcessorTest extends RepositoryTestCase {

    private static final String TAG_PREFIX = "test-test";
    private static final String V_1 = "1";
    private static final String V_2 = "2";
    private static final String V_3 = "3";
    private static final String NEXT_VERSION = "next";
    private static final String MASTER = "master";

    private RepositoryProcessor processor;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        processor = new RepositoryProcessor(false, "HEAD", NEXT_VERSION, "gitHubUrl",
                commit -> true, emptyList(), "/", TAG_PREFIX, mock(Log.class));
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void processEmptyRepository() throws Exception {
        final List<TagWrapper> tags = processor.process(db);
        assertThat(tags, is(empty()));
    }

    @Test
    public void processSimpleMainline() throws Exception {
        try (Git git = new Git(db)) {
            commitWithFile(git, "first");
            git.tag().setName(TAG_PREFIX + "-" + V_1).call();
            git.branchCreate().setName("branch").setStartPoint("HEAD").call();
            git.checkout().setName("branch").call();
            commitWithFile(git, "branch_1");
            final RevCommit branch = commitWithFile(git, "branch_2");
            git.checkout().setName(MASTER).call();
            git.merge().include(branch).setFastForward(FastForwardMode.NO_FF).setMessage("merge1").call();
            git.tag().setName(TAG_PREFIX + "-" + V_2).call();
        }

        final List<TagWrapper> tags = processor.process(db);
        assertThat(tags, hasSize(3));
        assertTag(tags.get(0), NEXT_VERSION, emptyMap());
        assertTag(tags.get(1), V_2, of("merge1", asList("branch_2", "branch_1")));
        assertTag(tags.get(2), V_1, of("first", emptyList()));
    }

    @Test
    public void processMainlineWithLongLivingBranches() throws Exception {
        try (Git git = new Git(db)) {
            commitWithFile(git, "first");
            git.tag().setName(TAG_PREFIX + "-" + V_1).call();
            git.branchCreate().setName("branch").setStartPoint("HEAD").call();
            git.checkout().setName("branch").call();
            commitWithFile(git, "branch_1");
            RevCommit branch = commitWithFile(git, "branch_2");
            git.checkout().setName(MASTER).call();
            git.merge().include(branch).setFastForward(FastForwardMode.NO_FF).setMessage("merge1").call();
            git.tag().setName(TAG_PREFIX + "-" + V_2).call();
            git.checkout().setName("branch").call();
            branch = commitWithFile(git, "branch_3");
            git.checkout().setName(MASTER).call();
            git.merge().include(branch).setFastForward(FastForwardMode.NO_FF).setMessage("merge2").call();
            git.tag().setName(TAG_PREFIX + "-" + V_3).call();
        }

        final List<TagWrapper> tags = processor.process(db);
        assertThat(tags, hasSize(4));
        assertTag(tags.get(0), NEXT_VERSION, emptyMap());
        assertTag(tags.get(1), V_3, of("merge2", singletonList("branch_3")));
        assertTag(tags.get(2), V_2, of("merge1", asList("branch_2", "branch_1")));
        assertTag(tags.get(3), V_1, of("first", emptyList()));
    }

    private RevCommit commitWithFile(Git git, String commit) throws IOException, GitAPIException {
        writeTrashFile(commit, commit);
        git.add().addFilepattern(commit).call();
        return git.commit().setMessage(commit).setAll(true).call();
    }

    private void assertTag(TagWrapper tag, String name, Map<String, List<String>> commits) {
        assertThat(tag.getName(), is(name));
        assertThat(tag.getCommits(), hasSize(commits.keySet().size()));
        final Map<String, List<String>> collect = tag.getCommits().stream()
                .collect(toMap(CommitWrapper::getTitle,
                        c -> c.getChildren().stream().map(CommitWrapper::getTitle).collect(toList())));
        assertThat(collect, is(commits));
    }

}

