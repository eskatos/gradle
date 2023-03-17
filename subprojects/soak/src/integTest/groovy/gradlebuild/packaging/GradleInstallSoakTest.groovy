/*
 * Copyright 2022 the original author or authors.
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

package gradlebuild.packaging

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.integtests.fixtures.executer.GradleContextualExecuter
import org.gradle.test.fixtures.file.TestFile
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

import java.nio.file.Path
import java.nio.file.Paths
import org.gradle.testkit.runner.GradleRunner


class GradleInstallSoakTest extends AbstractIntegrationSpec {

    private BuildResult runnerResult
    private TestFile target

    def setup(){
        target = temporaryFolder.getTestDirectory()
    }

    def "installs into empty dir"() {
        when:
        runInstallTask()

        then:
        assertSucceds()
    }

    def "installs to non-existing dir"(){
        given:
        def invalidPath = target.createDir()
        invalidPath.deleteDir()

        when:
        runInstallTask(invalidPath)

        then:
        assertSucceds()
    }

    def "installs into previous distribution"(){
        when:
        runInstallTask()
        runInstallTask()

        then:
        assertSucceds()
    }

    def "installs into something that looks like previous distribution"(){
        given:
        def binDir = target.createDir("bin")
        binDir.createFile("gradle").write("stub")
        binDir.createFile("gradle.exe").write("stub")

        def libDir = target.createDir("lib")
        libDir.createFile("gradle-8.0.2.jar").write("stub")
        libDir.createFile("all-deps-in-the-world-1.2.2.jar").write("stub")

        when:
        runInstallTask()

        then:
        assertSucceds()
    }

    def "does not install to file"(){
        given:
        def file = target.createFile("some file")
        file.write("some content")

        when:
        tryRunInstallTask(file)

        then:
        assertFails("Install directory $file is not valid: it is actually a file")
    }

    def "does not install to non-empty dir without lib"(){
        given:
        def binDir = target.createDir("bin")
        binDir.createFile("gradle").write("stub")
        binDir.createFile("gradle.exe").write("stub")

        when:
        tryRunInstallTask()

        then:
        assertFails("Install directory $target does not look like a Gradle installation. Cannot delete it to install.")
    }

    def "does not install to non-empty dir with empty lib"(){
        given:
        def binDir = target.createDir("bin")
        binDir.createFile("gradle").write("stub")
        binDir.createFile("gradle.exe").write("stub")

        def libDir = target.createDir("lib")

        when:
        tryRunInstallTask()

        then:
        assertFails("Install directory $target does not look like a Gradle installation. Cannot delete it to install.")
    }

    def "does not install to non-empty dir without gradle executables"(){
        given:
        def libDir = target.createDir("lib")
        libDir.createFile("gradle-8.0.2.jar").write("stub")
        libDir.createFile("all-deps-in-the-world-1.2.2.jar").write("stub")

        when:
        tryRunInstallTask()

        then:
        assertFails("Install directory $target does not look like a Gradle installation. Cannot delete it to install.")
    }

    def "does not install to non-empty dir without gradle executables and empty bin"(){
        given:
        def binDir = target.createDir("bin")
        def libDir = target.createDir("lib")
        libDir.createFile("gradle-8.0.2.jar").write("stub")
        libDir.createFile("all-deps-in-the-world-1.2.2.jar").write("stub")

        when:
        tryRunInstallTask()

        then:
        assertFails("Install directory $target does not look like a Gradle installation. Cannot delete it to install.")
    }

    def "does not install to dir with other executables"(){
        given:
        def binDir = target.createDir("bin")
        binDir.createFile("gradle").write("stub")
        binDir.createFile("gradle.exe").write("stub")
        binDir.createFile("python").write("stub")

        def libDir = target.createDir("lib")
        binDir.createFile("gradle-8.0.2.jar").write("stub")
        binDir.createFile("all-deps-in-the-world-1.2.2.jar").write("stub")

        when:
        tryRunInstallTask()

        then:
        assertFails("Install directory $target does not look like a Gradle installation. Cannot delete it to install.")
    }

    def "does not install to dir without jars"(){
        given:
        def binDir = target.createDir("bin")
        binDir.createFile("gradle").write("stub")
        binDir.createFile("gradle.exe").write("stub")

        def libDir = target.createDir("lib")
        binDir.createFile("all-deps-in-the-world-1.2.2.jar").write("stub")

        when:
        tryRunInstallTask()

        then:
        assertFails("Install directory $target does not look like a Gradle installation. Cannot delete it to install.")
    }

    private def gradleRoot = findRoot(Paths.get(".").normalize().toAbsolutePath()).toFile()

    private def findRoot(Path path) {
        if (path.resolve("version.txt").toFile().exists()) {
            return path
        } else if (path == null || path == path.parent) {
            throw IllegalStateException("Cannot find 'version.txt' file in root of repository")
        } else {
            return findRoot(path.parent)
        }
    }

    private def createRunner(TestFile targetPath){
        def runner = GradleRunner.create()
            .withProjectDir(gradleRoot)
            .forwardOutput()
            .withArguments("install", "-Pgradle_installPath=$targetPath")

        if (!GradleContextualExecuter.embedded) {
            runner.withGradleInstallation(buildContext.gradleHomeDir)
        }

        return runner
    }

    def runInstallTask(TestFile targetPath = target){
        runnerResult = createRunner(targetPath).build()
    }

    def tryRunInstallTask(TestFile targetPath = target){
        runnerResult = createRunner(targetPath).buildAndFail()
    }

    private def assertSucceds(){
        def taskResult = runnerResult.task(":distributions-full:install")
        return taskResult.outcome == TaskOutcome.SUCCESS
    }

    private def assertFails(String error){
        runnerResult.output.contains(error)
    }
}
