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

package org.eclipse.edc.spi.iam;

import org.eclipse.edc.policy.model.Policy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

public class VerificationContextTest {

    @Test
    void assertMandatoryPolicy() {
        assertThatNullPointerException().isThrownBy(() -> VerificationContext.Builder.newInstance()
                        .build())
                .withMessageContaining("Policy");
    }

    @Test
    void buildVerificationContext() {
        assertThatNoException().isThrownBy(() -> VerificationContext.Builder.newInstance()
                .policy(Policy.Builder.newInstance().build())
                .build());
    }

    @Test
    void buildVerificationContext_withAdditionalData() {

        var data = new TestData();

        var context = VerificationContext.Builder.newInstance()
                .policy(Policy.Builder.newInstance().build())
                .data(TestData.class, data)
                .build();

        assertThat(context.getContextData(TestData.class)).isEqualTo(data);
    }

    private static class TestData {
    }
}
