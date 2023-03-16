/*
 * Copyright 2023 the original author or authors.
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

package org.gradle.kotlin.dsl.normalization

import org.gradle.api.internal.cache.StringInterner
import org.gradle.api.internal.changedetection.state.ResourceSnapshotterCacheService
import org.gradle.api.internal.file.FileCollectionFactory
import org.gradle.api.internal.initialization.loadercache.DefaultClasspathHasher
import org.gradle.internal.classloader.ClasspathHasher
import org.gradle.internal.classpath.ClassPath
import org.gradle.internal.execution.FileCollectionSnapshotter
import org.gradle.internal.fingerprint.classpath.ClasspathFingerprinter
import org.gradle.internal.hash.HashCode
import javax.inject.Inject


internal
const val BUILDSCRIPT_COMPILE_AVOIDANCE_ENABLED = true


/**
 * A classpath hasher that understands Kotlin classpath.
 *
 * Provides classpath normalization that makes buildscript compilation avoidance work.
 */
class KotlinClasspathHasher @Inject constructor(
    cacheService: ResourceSnapshotterCacheService,
    fileCollectionSnapshotter: FileCollectionSnapshotter,
    stringInterner: StringInterner,
    fileCollectionFactory: FileCollectionFactory,
    classpathFingerprinter: ClasspathFingerprinter,
    private val jvmClasspathHasher: ClasspathHasher,
) {

    private
    val kotlinHasher = DefaultClasspathHasher(
        if (BUILDSCRIPT_COMPILE_AVOIDANCE_ENABLED) {
            KotlinCompileClasspathFingerprinter(
                cacheService,
                fileCollectionSnapshotter,
                stringInterner
            )
        } else {
            classpathFingerprinter
        },
        fileCollectionFactory
    )

    fun hash(classpath: ClassPath): HashCode =
        try {
            kotlinHasher.hash(classpath)
        } catch (ex: CompileAvoidanceException) {
            jvmClasspathHasher.hash(classpath)
        }
}
