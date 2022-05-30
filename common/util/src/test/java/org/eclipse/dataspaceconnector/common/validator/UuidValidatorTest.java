/*
 *  Copyright (c) 2022 ZF Friedrichshafen AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       ZF Friedrichshafen AG - Initial Implementation
 *
 */

package org.eclipse.dataspaceconnector.common.validator;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class UuidValidatorTest {

    UuidValidator uuidValidator = new UuidValidator();

    @Test
    void isValid_valid_uuid() {
        String uuid = UUID.randomUUID().toString();
        assertThat(uuidValidator.isValid(uuid, null)).isTrue();
    }

    @Test
    void isValid_invalid_uuid() {
        String uuid = "invalid-uuid-string";
        assertThat(uuidValidator.isValid(uuid, null)).isFalse();
    }
}
