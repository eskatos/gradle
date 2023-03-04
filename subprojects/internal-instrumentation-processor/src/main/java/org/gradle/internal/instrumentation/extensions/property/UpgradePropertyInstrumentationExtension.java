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

import org.gradle.internal.instrumentation.api.annotations.UpgradedClassesRegistry;
import org.gradle.internal.instrumentation.api.annotations.UpgradedProperty;
import org.gradle.internal.instrumentation.model.CallInterceptionRequest;
import org.gradle.internal.instrumentation.model.CallInterceptionRequestImpl;
import org.gradle.internal.instrumentation.model.CallableInfo;
import org.gradle.internal.instrumentation.model.CallableInfoImpl;
import org.gradle.internal.instrumentation.model.CallableKindInfo;
import org.gradle.internal.instrumentation.model.ImplementationInfoImpl;
import org.gradle.internal.instrumentation.model.ParameterInfo;
import org.gradle.internal.instrumentation.model.RequestExtra;
import org.gradle.internal.instrumentation.processor.extensibility.AnnotatedMethodReaderExtension;
import org.gradle.internal.instrumentation.processor.extensibility.ClassLevelAnnotationsContributor;
import org.gradle.internal.instrumentation.processor.modelreader.api.CallInterceptionRequestReader.Result.Success;
import org.objectweb.asm.Type;

import javax.lang.model.element.ExecutableElement;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.gradle.internal.instrumentation.processor.modelreader.impl.TypeUtils.extractMethodDescriptor;
import static org.gradle.internal.instrumentation.processor.modelreader.impl.TypeUtils.extractReturnType;
import static org.gradle.internal.instrumentation.processor.modelreader.impl.TypeUtils.extractType;

public class UpgradePropertyInstrumentationExtension implements
    AnnotatedMethodReaderExtension,
    ClassLevelAnnotationsContributor
    // CodeGeneratorContributor
{

//    @Override
//    public InstrumentationCodeGenerator contributeCodeGenerator() {
//        InterceptJvmCallsGenerator jvmGenerator = new InterceptJvmCallsGenerator();
//        return jvmGenerator::generateCodeForRequestedInterceptors;
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
//    }

    @Override
    public Collection<Result> readRequest(ExecutableElement input) {
        // read the annotations
        // produce the requests to intercept the property getter and setter (+ Groovy property access)
        // the returned requests should reference non-existing methods as interceptor implementations
        // like access_get_maxErrors and access_get_maxErrors
        // and they also need to store the information that they need those methods to be generated
        // (perhaps use the request extras)
        if (input.getAnnotation(UpgradedProperty.class) == null) {
            return Collections.emptySet();
        }

        List<RequestExtra> extras = new ArrayList<>();
        extras.add(new RequestExtra.OriginatingElement(input));
        extras.add(new RequestExtra.InterceptJvmCalls("org.gradle.internal.classpath.InterceptorDeclaration_JvmBytecodeImplCodeQuality2"));
        CallInterceptionRequest request = new CallInterceptionRequestImpl(extractCallableInfo(input), extractImplementationInfo(input), extras);
        Result result = new Success(request);
        System.out.println(input.getSimpleName());
        return Collections.singletonList(result);
    }

    private static ImplementationInfoImpl extractImplementationInfo(ExecutableElement input) {
        Type implementationOwner = extractType(input.getEnclosingElement().asType());
        String implementationName = input.getSimpleName().toString();
        String implementationDescriptor = extractMethodDescriptor(input);
        return new ImplementationInfoImpl(implementationOwner, implementationName, implementationDescriptor);
    }

    private static CallableInfo extractCallableInfo(ExecutableElement methodElement) {
        CallableKindInfo kindInfo = CallableKindInfo.INSTANCE_METHOD;
        Type owner = extractType(methodElement.getEnclosingElement().asType());
        // TODO Handle for property and setter differently
        String callableName = methodElement.getSimpleName().toString();
        Type returnType = extractReturnType(methodElement);
        // TODO handle setter differently
        List<ParameterInfo> parameterInfos = Collections.emptyList();
        return new CallableInfoImpl(kindInfo, owner, callableName, returnType, parameterInfos);
    }

    @Override
    public Collection<Class<? extends Annotation>> contributeClassLevelAnnotationTypes() {
        return Collections.singletonList(UpgradedClassesRegistry.class);
    }
}
