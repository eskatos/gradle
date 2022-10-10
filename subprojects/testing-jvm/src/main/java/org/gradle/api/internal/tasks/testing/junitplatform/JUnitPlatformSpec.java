/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.api.internal.tasks.testing.junitplatform;

import org.gradle.api.internal.tasks.testing.junit.AbstractJUnitSpec;
import org.gradle.api.tasks.testing.junitplatform.JUnitPlatformOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class JUnitPlatformSpec extends AbstractJUnitSpec {
    private final Set<String> includeEngines;
    private final Set<String> excludeEngines;
    private final Set<String> includeTags;
    private final Set<String> excludeTags;
    private final boolean isDryRun;
    public JUnitPlatformSpec(JUnitPlatformOptions options, Set<String> includedTests,
                             Set<String> excludedTests, Set<String> includedTestsCommandLine) {
        super(includedTests, excludedTests, includedTestsCommandLine);
        this.includeEngines = options.getIncludeEngines();
        this.excludeEngines = options.getExcludeEngines();
        this.includeTags = options.getIncludeTags();
        this.excludeTags = options.getExcludeTags();
        this.isDryRun = options.isDryRun();
    }

    public List<String> getIncludeEngines() {
        return new ArrayList<String>(includeEngines);
    }

    public List<String> getExcludeEngines() {
        return new ArrayList<String>(excludeEngines);
    }

    public List<String> getIncludeTags() {
        return new ArrayList<String>(includeTags);
    }

    public List<String> getExcludeTags() {
        return new ArrayList<String>(excludeTags);
    }

    public boolean isDryRun() {
        return isDryRun;
    }
}
