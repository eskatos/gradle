/*
 * Copyright 2019 the original author or authors.
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

package org.gradle.internal.file.impl

import org.gradle.test.fixtures.file.TestFile
import org.gradle.test.fixtures.condition.Requires
import org.gradle.test.fixtures.condition.UnitTestPreconditions

import static org.gradle.util.WindowsSymbolicLinkUtil.createWindowsSymbolicLink

@Requires(UnitTestPreconditions.Windows)
class WindowsSymbolicLinkDeleterTest extends AbstractSymlinkDeleterTest {
    @Override
    protected void createSymbolicLink(File link, TestFile target) {
        createWindowsSymbolicLink(link, target)
    }

    @Override
    protected boolean canCreateSymbolicLinkToFile() {
        return true
    }

    @Override
    protected boolean canCreateSymbolicLinkToDirectory() {
        return true
    }
}
