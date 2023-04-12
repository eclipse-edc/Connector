/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.junit.assertions;

import org.assertj.core.api.Assertions;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;

class AbstractResultAssertTest {

    @Test
    void isSucceeded_shouldReturnAssertOnContent() {
        var result = Result.success(3);

        assertThat(result).isSucceeded().isEqualTo(3);
    }

    @Test
    void isFailed_shouldReturnAssertOnFailure() {
        var result = Result.failure(new ArrayList<>());

        var failureAssert = assertThat(result).isFailed();
        Assertions.assertThat(failureAssert).isInstanceOf(FailureAssert.class);
    }
}
