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

package org.gradle.internal.instrumentation.extensions.property;

import org.gradle.internal.instrumentation.processor.codegen.InstrumentationCodeGenerator;
import org.gradle.internal.instrumentation.processor.codegen.InstrumentationCodeGenerator.GenerationResult;
import org.gradle.internal.instrumentation.processor.codegen.groovy.InterceptGroovyCallsGenerator;
import org.gradle.internal.instrumentation.processor.codegen.jvmbytecode.InterceptJvmCallsGenerator;
import org.gradle.internal.instrumentation.processor.extensibility.AnnotatedMethodReaderExtension;
import org.gradle.internal.instrumentation.processor.extensibility.CodeGeneratorContributor;

import javax.lang.model.element.ExecutableElement;
import java.util.Collection;
import java.util.Collections;

class PropertyExtension implements AnnotatedMethodReaderExtension, CodeGeneratorContributor {

    @Override
    public InstrumentationCodeGenerator contributeCodeGenerator() {
        InterceptJvmCallsGenerator jvmGenerator = new InterceptJvmCallsGenerator();
        return jvmGenerator::generateCodeForRequestedInterceptors;
//        return new CodeGeneratorContributor() {
//            @Override
//            public InstrumentationCodeGenerator contributeCodeGenerator() {
//                // look for the extra data in the requests – find the ones that need accessor
//                // implementations, then generate them, like:
//                //
//                // class ChekstyleImpls {
//                //     public static access_get_maxErrors(Checkstyle self) {
//                //         self.getMaxErrors().get()
//                //     }
//                //
//                //     public static access_set_maxErrors(Checkstyle self, int value) {
//                //         ...
//                //     }
//                // }
//                return null;
//            }
//        };
    }

    @Override
    public Collection<Result> readRequest(ExecutableElement input) {
        // read the annotations
        // produce the requests to intercept the property getter and setter (+ Groovy property access)
        // the returned requests should reference non-existing methods as interceptor implementations
        // like access_get_maxErrors and access_get_maxErrors
        // and they also need to store the information that they need those methods to be generated
        // (perhaps use the request extras)
        return Collections.emptySet();
    }
}
