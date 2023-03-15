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
import static org.gradle.internal.instrumentation.extensions.property.PropertyUpgradeAnnotatedMethodReaderExtension.INTERCEPTOR_GROOVY_DECLARATION_CLASS_NAME
import static org.gradle.internal.instrumentation.extensions.property.PropertyUpgradeAnnotatedMethodReaderExtension.INTERCEPTOR_JVM_DECLARATION_CLASS_NAME

class PropertyUpgradeCodeGenTest extends Specification {

    def "should auto generate adapter for upgraded property with originalType"() {
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

    def "should auto generate adapter for upgraded property with type #upgradedType"() {
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
                import org.gradle.internal.instrumentation.api.annotations.UpgradedProperty.UpgradedGroovyProperty;

                public class TaskCustomAccessors {
                    @UpgradedSetter(forProperty = "maxErrors")
                    public static void access_setMaxErrors(Task self, int value) {
                    }
                    @UpgradedSetter(forProperty = "maxErrors")
                    public static Task access_setMaxErrors(Task self, Object value) {
                        return self;
                    }

                    @UpgradedGetter(forProperty = "maxErrors")
                    @UpgradedGroovyProperty(forProperty = "maxErrors")
                    public static int access_getMaxErrors(Task self) {
                        return self.getMaxErrors().get();
                    }
                }
        """)

        when:
        Compilation compilation = compile(givenSource, givenAccessorsSource)

        then:
        def jvmInterceptorsClassName = INTERCEPTOR_JVM_DECLARATION_CLASS_NAME
        def expectedJvmInterceptors = JavaFileObjects.forSourceLines(jvmInterceptorsClassName, """
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
        def groovyInterceptorsClassName = INTERCEPTOR_GROOVY_DECLARATION_CLASS_NAME
        def expectedGroovyInterceptors = JavaFileObjects.forSourceLines(groovyInterceptorsClassName, """
            package org.gradle.internal.classpath;

            import java.lang.Object;
            import java.lang.Override;
            import java.lang.String;
            import java.lang.Throwable;
            import java.util.Arrays;
            import java.util.List;
            import org.gradle.internal.classpath.intercept.CallInterceptor;
            import org.gradle.internal.classpath.intercept.InterceptScope;
            import org.gradle.internal.classpath.intercept.Invocation;
            import org.gradle.test.Task;
            import org.gradle.test.TaskCustomAccessors;

            class InterceptorDeclaration_GroovyInterceptorsImplPropertyUpgrades {
                public static List<CallInterceptor> getCallInterceptors() {
                    return Arrays.asList(
                        new GetMaxErrorsCallInterceptor()
                    );
                }

                /**
                 * Intercepts the following declarations:<ul>
                 *
                 * <li> Groovy property getter {@link org.gradle.test.Task#maxErrors}
                 *      with {@link TaskCustomAccessors#access_getMaxErrors(Task)}
                 *
                 * </ul>
                 */
                private static class GetMaxErrorsCallInterceptor extends CallInterceptor {
                    public GetMaxErrorsCallInterceptor() {
                        super(InterceptScope.readsOfPropertiesNamed("maxErrors"), InterceptScope.methodsNamed("getMaxErrors"));
                    }

                    @Override
                    protected Object doIntercept(Invocation invocation, String consumer) throws Throwable {
                        Object receiver = invocation.getReceiver();
                        if (receiver instanceof Task) {
                            Task receiverTyped = (Task) receiver;
                            if (invocation.getArgsCount() == 0) {
                                return TaskCustomAccessors.access_getMaxErrors(receiverTyped);
                            }
                        }
                        return invocation.callOriginal();
                    }
                }
            }
        """)
        assertThat(compilation).succeededWithoutWarnings()
        assertThat(compilation)
            .generatedSourceFile(jvmInterceptorsClassName)
            .containsElementsIn(expectedJvmInterceptors)
        assertThat(compilation)
            .generatedSourceFile(groovyInterceptorsClassName)
            .hasSourceEquivalentTo(expectedGroovyInterceptors)
    }

    def "should fail compilation with errors if custom accessors has errors"() {
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
                public abstract Property<Integer> getMaxWarnings();
                @UpgradedProperty(accessors = TaskCustomAccessors.class)
                public abstract Property<Integer> getMaxErrors();
                @UpgradedProperty(accessors = TaskCustomAccessors.class)
                public abstract Property<Integer> getMaxSuccesses();
                @UpgradedProperty(accessors = TaskCustomAccessors.class)
                public abstract Property<Integer> getMinErrors();
            }
        """)
        def givenAccessorsSource = JavaFileObjects.forSourceString("org.gradle.test.TaskCustomAccessors", """
                package org.gradle.test;

                import org.gradle.internal.instrumentation.api.annotations.UpgradedProperty.UpgradedGetter;
                import org.gradle.internal.instrumentation.api.annotations.UpgradedProperty.UpgradedSetter;
                import org.gradle.internal.instrumentation.api.annotations.UpgradedProperty.UpgradedGroovyProperty;

                public class TaskCustomAccessors {
                    @UpgradedSetter(forProperty = "maxErrors")
                    public static void access_setMaxErrors() {
                    }
                    @UpgradedSetter(forProperty = "maxErrors")
                    public static Task access_setMaxErrors(Task self, Object value) {
                        return self;
                    }
                    @UpgradedSetter(forProperty = "maxErrors")
                    public static void access_setMaxErrors(int value) {
                    }
                    @UpgradedGetter(forProperty = "maxErrors")
                    @UpgradedGroovyProperty(forProperty = "maxErrors")
                    public static int access_getMaxErrors(Task self, int arg0) {
                        return self.getMaxErrors().get();
                    }
                    @UpgradedSetter(forProperty = "maxSuccesses")
                    public static int setMaxSuccesses(Task self) {
                        return 0;
                    }
                    @UpgradedGetter(forProperty = "maxSuccesses")
                    @UpgradedGroovyProperty(forProperty = "maxSuccesses")
                    public static void access_getMaxSuccesses(Task self) {
                    }
                    @UpgradedGetter(forProperty = "minErrors")
                    @UpgradedSetter(forProperty = "minErrors")
                    @UpgradedGroovyProperty(forProperty = "minErrors")
                    public static int access_setAndGetMinErrors(Task self) {
                        return 0;
                    }
                }
        """)

        when:
        Compilation compilation = compile(givenSource, givenAccessorsSource)

        then:
        assertThat(compilation).failed()
        assertThat(compilation).hadErrorCount(10)
        assertThat(compilation).hadErrorContaining("No custom accessors found for property: org.gradle.test.Task.getMaxWarnings().")
        assertThat(compilation).hadErrorContaining("No accessors annotated with @UpgradedGroovyProperty for property: org.gradle.test.Task.getMaxWarnings(). There should be 1 accessor with that annotation.")
        assertThat(compilation).hadErrorContaining("No accessors annotated with @UpgradedGetter for property: org.gradle.test.Task.getMaxWarnings(). There should be at least 1 accessor with that annotation.")
        assertThat(compilation).hadErrorContaining("First parameter for accessor method 'org.gradle.test.TaskCustomAccessors.access_setMaxErrors()' should be of type 'org.gradle.test.Task', but this method has no parameter.")
        assertThat(compilation).hadErrorContaining("First parameter for accessor method 'org.gradle.test.TaskCustomAccessors.access_setMaxErrors(int)' should be of type 'org.gradle.test.Task', but is 'int'.")
        assertThat(compilation).hadErrorContaining("Too many parameters for accessor method 'org.gradle.test.TaskCustomAccessors.access_getMaxErrors(org.gradle.test.Task,int)' annotated with @UpgradedGroovyProperty or @UpgradedGetter. There should be just 1 parameter of type 'org.gradle.test.Task', but this method has also additional parameters.")
        assertThat(compilation).hadErrorContaining("Accessor method 'org.gradle.test.TaskCustomAccessors.setMaxSuccesses(org.gradle.test.Task)' name should start with 'access_'.")
        assertThat(compilation).hadErrorContaining("Accessor method 'org.gradle.test.TaskCustomAccessors.access_getMaxSuccesses(org.gradle.test.Task)' annotated with @UpgradedGroovyProperty or @UpgradedGetter should not have return type 'void'.")
        assertThat(compilation).hadErrorContaining("Accessor method 'org.gradle.test.TaskCustomAccessors.access_setAndGetMinErrors(org.gradle.test.Task)' should not have have @UpgradedGetter and @UpgradedSetter annotation.")
        assertThat(compilation).hadErrorContaining("Accessor method 'org.gradle.test.TaskCustomAccessors.access_setAndGetMinErrors(org.gradle.test.Task)' should not have have @UpgradedGroovyProperty and @UpgradedSetter annotation.")
    }

    def "should fail compilation with error if custom accessor method is for multiple properties"() {
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
                import org.gradle.internal.instrumentation.api.annotations.UpgradedProperty.UpgradedGroovyProperty;

                public class TaskCustomAccessors {
                    @UpgradedGetter(forProperty = "maxErrors2")
                    @UpgradedGroovyProperty(forProperty = "maxErrors")
                    public static int access_getMaxErrors(Task self) {
                        return self.getMaxErrors().get();
                    }
                }
        """)

        when:
        Compilation compilation = compile(givenSource, givenAccessorsSource)

        then:
        assertThat(compilation).failed()
        assertThat(compilation).hadErrorCount(1)
        assertThat(compilation).hadErrorContaining("Accessor method 'org.gradle.test.TaskCustomAccessors.access_getMaxErrors(org.gradle.test.Task)' is used multiple different properties. That use case is not supported.")
    }

    private static Compilation compile(JavaFileObject... fileObjects) {
        return javac()
            .withOptions("--release=8")
            .withProcessors(new ConfigurationCacheInstrumentationProcessor())
            .compile(fileObjects)
    }
}
