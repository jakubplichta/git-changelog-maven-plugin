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

import java.util.List;

/**
 * Main object representing whole GIT Change Log. Object is used as a model within template.
 */
public class ChangeLog {

    private final String reportTitle;
    private final List<TagWrapper> tags;

    public ChangeLog(String reportTitle, List<TagWrapper> tags) {
        this.reportTitle = reportTitle;
        this.tags = tags;
    }

    public String getReportTitle() {
        return reportTitle;
    }

    public List<TagWrapper> getTags() {
        return tags;
    }

}
