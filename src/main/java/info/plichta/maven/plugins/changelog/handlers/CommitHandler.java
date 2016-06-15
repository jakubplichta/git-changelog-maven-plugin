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

import info.plichta.maven.plugins.changelog.model.CommitWrapper;

/**
 * {@link CommitHandler} is responsible for processing GIT commits. Its implementation may change {@link CommitWrapper}
 * data and add any additional ones.
 */
public interface CommitHandler {

    /**
     * Takes provided commit, processes its information and enhances it.
     *
     * @param commit commit to be processed
     */
    void handle(CommitWrapper commit);
}
