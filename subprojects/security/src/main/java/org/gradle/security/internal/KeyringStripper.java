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

package org.gradle.security.internal;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.operator.KeyFingerPrintCalculator;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * A utility class to strip unnecessary information from a keyring
 */
public class KeyringStripper {

    public static PGPPublicKeyRing strip(PGPPublicKeyRing keyring, KeyFingerPrintCalculator fingerprintCalculator) {
        List<PGPPublicKey> strippedKeys = StreamSupport
            .stream(keyring.spliterator(), false)
            .map(key -> {
                try {
                    return new PGPPublicKey(key.getPublicKeyPacket(), fingerprintCalculator);
                } catch (PGPException e) {
                    throw new RuntimeException(e);
                }
            })
            .collect(Collectors.toList());

        return new PGPPublicKeyRing(strippedKeys);
    }
}
