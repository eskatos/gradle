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

package org.gradle.internal.instrumentation.extensions.property

import com.google.testing.compile.Compilation
import com.google.testing.compile.JavaFileObjects
import org.gradle.internal.instrumentation.processor.ConfigurationCacheInstrumentationProcessor
import spock.lang.Specification

import javax.tools.JavaFileObject

import static com.google.testing.compile.CompilationSubject.assertThat
import static com.google.testing.compile.Compiler.javac
import static org.gradle.internal.instrumentation.extensions.property.PropertyUpgradeAnnotatedMethodReaderExtension.INTERCEPTOR_JVM_DECLARATION_CLASS_NAME

class PropertyUpgradeClassGeneratorTest extends Specification {

    def "should generate adapter for upgraded property with originalType"() {
        given:
        def fQName = "org.gradle.test.Task"
        def givenSource = JavaFileObjects.forSourceString(fQName, """
            package org.gradle.test;

            import org.gradle.api.provider.Property;
            import org.gradle.internal.instrumentation.api.annotations.VisitForInstrumentation;
            import org.gradle.internal.instrumentation.api.annotations.UpgradedProperty;

            @VisitForInstrumentation(value = {Task.class})
            public abstract class Task {
                @UpgradedProperty(originalType = int.class)
                public abstract Property<Integer> getMaxErrors();
            }
        """)

        when:
        Compilation compilation = compile(givenSource)

        then:
        def generatedClassName =  "org.gradle.internal.instrumentation.Task_Adapter"
        def expectedOutput = JavaFileObjects.forSourceLines(generatedClassName, """
            package org.gradle.internal.instrumentation;
            import org.gradle.test.Task;

            public class Task_Adapter {
                public static int access_get_maxErrors(Task self) {
                    return self.getMaxErrors().get();
                }

                public static void access_set_maxErrors(Task self, int arg0) {
                    self.getMaxErrors().set(arg0);
                }
            }
        """)
        assertThat(compilation).succeededWithoutWarnings()
        assertThat(compilation)
            .generatedSourceFile(generatedClassName)
            .hasSourceEquivalentTo(expectedOutput)
    }

    def "should generate adapter for upgraded property with type #upgradedType"() {
        given:
        def fQName = "org.gradle.test.Task"
        def givenSource = JavaFileObjects.forSourceString(fQName, """
            package org.gradle.test;

            import org.gradle.api.provider.*;
            import org.gradle.api.file.*;
            import org.gradle.internal.instrumentation.api.annotations.VisitForInstrumentation;
            import org.gradle.internal.instrumentation.api.annotations.UpgradedProperty;

            @VisitForInstrumentation(value = {Task.class})
            public abstract class Task {
                @UpgradedProperty
                public abstract $upgradedType getProperty();
            }
        """)

        when:
        Compilation compilation = compile(givenSource)

        then:
        def generatedClassName = "org.gradle.internal.instrumentation.Task_Adapter"
        def expectedOutput = JavaFileObjects.forSourceLines(generatedClassName, """
            package org.gradle.internal.instrumentation;
            import $fullImport;
            import org.gradle.test.Task;

            public class Task_Adapter {
                public static $originalType access_get_property(Task self) {
                    return self.getProperty()$getCall;
                }

                public static void access_set_property(Task self, $originalType arg0) {
                    self.getProperty()$setCall;
                }
            }
        """)
        assertThat(compilation).succeededWithoutWarnings()
        assertThat(compilation)
            .generatedSourceFile(generatedClassName)
            .hasSourceEquivalentTo(expectedOutput)

        where:
        upgradedType                  | originalType     | getCall              | setCall            | fullImport
        "Property<Integer>"           | "Integer"        | ".get()"             | ".set(arg0)"       | "java.lang.Integer"
        "Property<String>"            | "String"         | ".get()"             | ".set(arg0)"       | "java.lang.String"
        "ListProperty<String>"        | "List"           | ".get()"             | ".set(arg0)"       | "java.util.List"
        "MapProperty<String, String>" | "Map"            | ".get()"             | ".set(arg0)"       | "java.util.Map"
        "RegularFileProperty"         | "File"           | ".getAsFile().get()" | ".fileValue(arg0)" | "java.io.File"
        "DirectoryProperty"           | "File"           | ".getAsFile().get()" | ".fileValue(arg0)" | "java.io.File"
        "ConfigurableFileCollection"  | "FileCollection" | ""                   | ".setFrom(arg0)"   | "org.gradle.api.file.FileCollection"
    }


    def "should generate interceptors for custom accessors"() {
        given:
        def fQName = "org.gradle.test.Task"
        def givenSource = JavaFileObjects.forSourceString(fQName, """
            package org.gradle.test;

            import org.gradle.api.provider.Property;
            import org.gradle.internal.instrumentation.api.annotations.VisitForInstrumentation;
            import org.gradle.internal.instrumentation.api.annotations.UpgradedProperty;
            import org.gradle.internal.instrumentation.api.annotations.UpgradedProperty.UpgradedGetter;
            import org.gradle.internal.instrumentation.api.annotations.UpgradedProperty.UpgradedSetter;

            @VisitForInstrumentation(value = {Task.class})
            public abstract class Task {
                @UpgradedProperty(accessors = TaskCustomAccessors.class)
                public abstract Property<Integer> getMaxErrors();
            }
        """)
        def givenAccessorsSource = JavaFileObjects.forSourceString("org.gradle.test.TaskCustomAccessors", """
                package org.gradle.test;

                import org.gradle.internal.instrumentation.api.annotations.UpgradedProperty.UpgradedGetter;
                import org.gradle.internal.instrumentation.api.annotations.UpgradedProperty.UpgradedSetter;

                public class TaskCustomAccessors {
                    @UpgradedSetter(forProperty = "maxErrors")
                    public static void access_setMaxErrors(Task self, int value) {
                    }
                    @UpgradedSetter(forProperty = "maxErrors")
                    public static Task access_setMaxErrors(Task self, Object value) {
                        return self;
                    }
                    @UpgradedGetter(forProperty = "maxErrors")
                    public static int access_getMaxErrors(Task self) {
                        return self.getMaxErrors().get();
                    }
                }
        """)

        when:
        Compilation compilation = compile(givenSource, givenAccessorsSource)

        then:
        def generatedClassName = INTERCEPTOR_JVM_DECLARATION_CLASS_NAME
        def expectedVisitMethodIns = JavaFileObjects.forSourceLines(generatedClassName, """
            package org.gradle.internal.classpath;

            import java.lang.Override;
            import java.lang.String;
            import java.util.function.Supplier;
            import org.gradle.internal.instrumentation.api.jvmbytecode.JvmBytecodeCallInterceptor;
            import org.gradle.model.internal.asm.MethodVisitorScope;
            import org.gradle.test.Task;
            import org.gradle.test.TaskCustomAccessors;
            import org.objectweb.asm.Opcodes;
            import org.objectweb.asm.Type;
            import org.objectweb.asm.tree.MethodNode;

            class InterceptorDeclaration_JvmBytecodeImplPropertyUpgrades extends MethodVisitorScope implements JvmBytecodeCallInterceptor {
                private static final Type TASK_CUSTOM_ACCESSORS_TYPE = Type.getType(TaskCustomAccessors.class);

                @Override
                public boolean visitMethodInsn(String className, int opcode, String owner, String name,
                        String descriptor, boolean isInterface, Supplier<MethodNode> readMethodNode) {
                    if (owner.equals("org/gradle/test/Task")) {
                        /**
                         * Intercepting instance method: {@link org.gradle.test.Task#getMaxErrors()}
                         * Intercepted by {@link TaskCustomAccessors#access_getMaxErrors(Task)}
                        */
                        if (name.equals("getMaxErrors") && descriptor.equals("()I") && opcode == Opcodes.INVOKEVIRTUAL) {
                            _INVOKESTATIC(TASK_CUSTOM_ACCESSORS_TYPE, "access_getMaxErrors", "(Lorg/gradle/test/Task;)I");
                            return true;
                        }
                        /**
                         * Intercepting instance method: {@link org.gradle.test.Task#setMaxErrors(int)}
                         * Intercepted by {@link TaskCustomAccessors#access_setMaxErrors(Task, int)}
                        */
                        if (name.equals("setMaxErrors") && descriptor.equals("(I)V") && opcode == Opcodes.INVOKEVIRTUAL) {
                            _INVOKESTATIC(TASK_CUSTOM_ACCESSORS_TYPE, "access_setMaxErrors", "(Lorg/gradle/test/Task;I)V");
                            return true;
                        }
                        /**
                         * Intercepting instance method: {@link org.gradle.test.Task#setMaxErrors(Object)}
                         * Intercepted by {@link TaskCustomAccessors#access_setMaxErrors(Task, Object)}
                        */
                        if (name.equals("setMaxErrors") && descriptor.equals("(Ljava/lang/Object;)Lorg/gradle/test/Task;") && opcode == Opcodes.INVOKEVIRTUAL) {
                            _INVOKESTATIC(TASK_CUSTOM_ACCESSORS_TYPE, "access_setMaxErrors", "(Lorg/gradle/test/Task;Ljava/lang/Object;)Lorg/gradle/test/Task;");
                            return true;
                        }
                    }
                    return false;
                }
            }
        """)
        assertThat(compilation).succeededWithoutWarnings()
        assertThat(compilation)
                .generatedSourceFile(generatedClassName)
                .containsElementsIn(expectedVisitMethodIns)
    }

    private static Compilation compile(JavaFileObject... fileObjects) {
        return javac()
            .withOptions("--release=8")
            .withProcessors(new ConfigurationCacheInstrumentationProcessor())
            .compile(fileObjects)
    }
}
