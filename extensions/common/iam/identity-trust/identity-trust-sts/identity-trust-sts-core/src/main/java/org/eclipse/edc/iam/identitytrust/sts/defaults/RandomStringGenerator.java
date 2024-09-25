/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.iam.identitytrust.sts.defaults;

import org.eclipse.edc.iam.identitytrust.sts.spi.service.StsClientSecretGenerator;
import org.jetbrains.annotations.Nullable;

import java.security.SecureRandom;

/**
 * Default client secret generator that creates an alpha-numeric string of length {@link RandomStringGenerator#DEFAULT_CLIENT_SECRET_LENGTH}
 * (16).
 */
public class RandomStringGenerator implements StsClientSecretGenerator {
    public static final int DEFAULT_CLIENT_SECRET_LENGTH = 16;

    @Override
    public String generateClientSecret(@Nullable Object parameters) {
        // algorithm taken from https://www.baeldung.com/java-random-string
        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'
        var random = new SecureRandom();

        return random.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(DEFAULT_CLIENT_SECRET_LENGTH)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }
}
