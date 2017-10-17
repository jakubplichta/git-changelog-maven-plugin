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

import org.eclipse.jgit.revwalk.RevCommit;

import java.util.Date;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class CommitFilter implements Predicate<RevCommit> {
    private final Predicate<RevCommit> predicate;

    private static final long MILLISECONDS = 1000;

    CommitFilter(final String include, final String exclude, final Date ignoreOlderThen) {
        final Pattern includePattern = Pattern.compile(include, Pattern.MULTILINE | Pattern.DOTALL);
        final Optional<Pattern> excludePattern = Optional.ofNullable(exclude).map((regex) -> Pattern.compile(regex, Pattern.MULTILINE | Pattern.DOTALL));


        final Predicate<RevCommit> includePred =
                revCommit -> includePattern.matcher(revCommit.getFullMessage()).matches();
        final Predicate<RevCommit> excludePred =
                revCommit -> excludePattern.map(p -> p.matcher(revCommit.getFullMessage()).matches()).orElse(false);
        final int maxTime = getMinimumCommitTime(ignoreOlderThen);
        final Predicate<RevCommit> timePred = revCommit -> revCommit.getCommitTime() >= maxTime;


        predicate = includePred.and(excludePred.negate()).and(timePred);
    }

    private int getMinimumCommitTime(Date ignoreOlderThen) {
        if (ignoreOlderThen != null) {
            return (int) (ignoreOlderThen.getTime() / MILLISECONDS);
        } else {
            return 0;
        }
    }

    @Override
    public boolean test(RevCommit revCommit) {
        return predicate.test(revCommit);
    }
}
