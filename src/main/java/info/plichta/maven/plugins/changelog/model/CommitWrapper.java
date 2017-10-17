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

package info.plichta.maven.plugins.changelog.model;

import org.eclipse.jgit.revwalk.RevCommit;

import java.text.SimpleDateFormat;
import java.util.*;

import static org.apache.commons.lang3.StringUtils.left;
import static org.apache.commons.lang3.StringUtils.stripEnd;

/**
 * Model class representing one GIT commit.
 */
public class CommitWrapper {


    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final long MILLISECONDS = 1000L;


    private final String gitHubUrl;
    private final RevCommit commit;
    private String title;

    private final Map<String, Object> extensions = new HashMap<>();
    private final List<CommitWrapper> children = new ArrayList<>();

    public CommitWrapper(RevCommit commit, String gitHubUrl) {
        this.gitHubUrl = gitHubUrl;
        this.commit = commit;
        this.title = commit.getShortMessage();
    }

    public RevCommit getCommit() {
        return commit;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getShortHash() {
        return left(commit.getName(), 7);
    }

    public String getCommitLink() {
        return stripEnd(gitHubUrl, "/") + "/commit/" + commit.getName();
    }

    public List<CommitWrapper> getChildren() {
        return children;
    }

    public Map<String, Object> getExtensions() {
        return extensions;
    }

    public String getCommitTime() {
        return DATE_FORMAT.format(new Date(commit.getCommitTime() * MILLISECONDS));
    }
}
