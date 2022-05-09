/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.aws.dataplane.s3.validation;

import org.eclipse.dataspaceconnector.spi.result.Result;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationRuleTest {

    @Test
    void shouldBeComposedWithAndOperator() {
        ValidationRule<String> notEmpty = s -> s.isEmpty() ? Result.failure("string is empty") : Result.success();
        ValidationRule<String> shorterThan10 = s -> s.length() < 10 ? Result.success() : Result.failure("string is not shorter than 10");

        var composed = notEmpty.and(shorterThan10);

        assertThat(composed.apply("valid").succeeded());
        assertThat(composed.apply("").failed());
        assertThat(composed.apply("").getFailureMessages()).containsExactly("string is empty");
        assertThat(composed.apply("toolongforthis").failed());
        assertThat(composed.apply("toolongforthis").getFailureMessages()).containsExactly("string is not shorter than 10");
    }
}