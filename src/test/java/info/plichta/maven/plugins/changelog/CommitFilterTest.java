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

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.junit.RepositoryTestCase;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class CommitFilterTest extends RepositoryTestCase {

    private RevCommit commit;
    private RevCommit commit2;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        try (Git git = new Git(db)) {
            commit = git.commit().setMessage("Commit").call();
            commit2 = git.commit().setMessage("Test").call();

        }
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testIncludeAllNoExclude() {
        final CommitFilter filter = new CommitFilter(".*", null, null);
        assertThat(filter.test(commit), is(true));
        assertThat(filter.test(commit2), is(true));
    }

    @Test
    public void testExcludeAll() {
        final CommitFilter filter = new CommitFilter(".*", ".*", null);
        assertThat(filter.test(commit), is(false));
        assertThat(filter.test(commit2), is(false));
    }

    @Test
    public void testIncludeNone() {
        final CommitFilter filter = new CommitFilter("", null, null);
        assertThat(filter.test(commit), is(false));
        assertThat(filter.test(commit2), is(false));
    }

    @Test
    public void testExcludeSome() {
        final CommitFilter filter = new CommitFilter(".*", ".*i.*", null);
        assertThat(filter.test(commit), is(false));
        assertThat(filter.test(commit2), is(true));
    }

    @Test
    public void testExcludeAllByTime() {
        final CommitFilter filter = new CommitFilter(".*", null, LocalDateTime.now());
        assertThat(filter.test(commit), is(false));
        assertThat(filter.test(commit2), is(false));
    }

    @Test
    public void testIncludeAllByTime() {
        final CommitFilter filter = new CommitFilter(".*", null, LocalDateTime.of(2000, 12, 1, 0, 0));
        assertThat(filter.test(commit), is(true));
        assertThat(filter.test(commit2), is(true));
    }
}