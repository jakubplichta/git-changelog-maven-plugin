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

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import info.plichta.maven.plugins.changelog.model.ChangeLog;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class ChangeLogWriter {

    private final Log log;
    private final Mustache mustache;

    public ChangeLogWriter(String template, Log log) {
        this.log = log;

        final DefaultMustacheFactory mustacheFactory = new DefaultMustacheFactory();
        mustache = mustacheFactory.compile(template);
    }

    public void write(File target, ChangeLog changeLog) {
        log.info("Writing changelog to file " + target);
        try (PrintWriter writer = new PrintWriter(target)) {
            mustache.execute(writer, changeLog);
        } catch (FileNotFoundException e) {
            // this should not happen
            throw new RuntimeException(e);
        }
    }
}
