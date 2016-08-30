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
import org.eclipse.jgit.junit.RepositoryTestCase;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static com.google.common.collect.ImmutableMap.of;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class RepositoryProcessorTest extends RepositoryTestCase {

    private static final String V_1 = "1";
    private static final String V_2 = "2";
    private static final String V_3 = "3";
    private static final String NEXT_VERSION = "next";
    private static final String MASTER = "master";

    private RepositoryProcessor processor;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        processor = new RepositoryProcessor(false, "HEAD", NEXT_VERSION, "gitHubUrl", commit -> true, emptyList(), mock(Log.class));
    }

    @Test
    public void processEmptyRepository() throws Exception {
        final List<TagWrapper> tags = processor.process(db);
        assertThat(tags, is(empty()));
    }

    @Test
    public void processSimpleMainline() throws Exception {
        try (Git git = new Git(db)) {
            git.commit().setMessage("first").call();
            git.tag().setName(V_1).call();
            git.branchCreate().setName("branch").setStartPoint("HEAD").call();
            git.checkout().setName("branch").call();
            git.commit().setMessage("branch_1").call();
            final RevCommit branch = git.commit().setMessage("branch_2").call();
            git.checkout().setName(MASTER).call();
            git.merge().include(branch).setFastForward(FastForwardMode.NO_FF).setMessage("merge1").call();
            git.tag().setName(V_2).call();
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
            git.commit().setMessage("first").call();
            git.tag().setName(V_1).call();
            git.branchCreate().setName("branch").setStartPoint("HEAD").call();
            git.checkout().setName("branch").call();
            git.commit().setMessage("branch_1").call();
            RevCommit branch = git.commit().setMessage("branch_2").call();
            git.checkout().setName(MASTER).call();
            git.merge().include(branch).setFastForward(FastForwardMode.NO_FF).setMessage("merge1").call();
            git.tag().setName(V_2).call();
            git.checkout().setName("branch").call();
            branch = git.commit().setMessage("branch_3").call();
            git.checkout().setName(MASTER).call();
            git.merge().include(branch).setFastForward(FastForwardMode.NO_FF).setMessage("merge2").call();
            git.tag().setName(V_3).call();
        }

        final List<TagWrapper> tags = processor.process(db);
        assertThat(tags, hasSize(4));
        assertTag(tags.get(0), NEXT_VERSION, emptyMap());
        assertTag(tags.get(1), V_3, of("merge2", singletonList("branch_3")));
        assertTag(tags.get(2), V_2, of("merge1", asList("branch_2", "branch_1")));
        assertTag(tags.get(3), V_1, of("first", emptyList()));
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

