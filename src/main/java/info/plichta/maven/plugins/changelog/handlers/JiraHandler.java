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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link CommitHandler} capable of detecting Jira references in commit messages.
 */
public class JiraHandler implements CommitHandler {
    private static final Pattern PATTERN = Pattern.compile("([A-Z]+-[0-9]+)");
    private final String jiraServer;

    public JiraHandler(String jiraServer) {
        this.jiraServer = jiraServer;
    }

    @Override
    public void handle(CommitWrapper commit) {
        final Matcher matcher = PATTERN.matcher(commit.getTitle());
        final JiraIssue jira = new JiraIssue();
        int lastEnd = 0;
        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                jira.getTitle().add(new TitleToken(commit.getTitle().substring(lastEnd, matcher.start()), null));
            }
            final String issue = matcher.group(1);
            lastEnd = matcher.end();
            jira.getTitle().add(new TitleToken(issue, new JiraLink(issue, jiraServer + "/browse/" + issue)));
        }
        jira.getTitle().add(new TitleToken(commit.getTitle().substring(lastEnd, commit.getTitle().length()), null));
        if (jira.getTitle().size() > 1) {
            commit.getExtensions().put("jira", jira);
        }
    }

    public static class JiraIssue {
        private final List<TitleToken> title = new ArrayList<>();

        public List<TitleToken> getTitle() {
            return title;
        }
    }

    public static class TitleToken {
        private final String token;
        private final JiraLink link;

        public TitleToken(String token, JiraLink link) {
            this.token = token;
            this.link = link;
        }

        public String getToken() {
            return token;
        }

        public JiraLink getLink() {
            return link;
        }
    }

    public static class JiraLink {
        private final String id;
        private final String link;

        public JiraLink(String id, String link) {
            this.id = id;
            this.link = link;
        }

        public String getId() {
            return id;
        }

        public String getLink() {
            return link;
        }
    }
}
