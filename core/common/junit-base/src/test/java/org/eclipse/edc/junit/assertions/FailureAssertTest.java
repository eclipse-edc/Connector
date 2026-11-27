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

import org.eclipse.edc.spi.result.Failure;
import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.Collections.emptyList;

class FailureAssertTest {

    @Test
    void messages_shouldAssertOnMessageList() {
        FailureAssert.assertThat(new Failure(emptyList())).messages().isEmpty();
        FailureAssert.assertThat(new Failure(List.of("one", "two"))).messages().hasSize(2);
    }

    @Test
    void detail_shouldAssertOnDetail() {
        FailureAssert.assertThat(new Failure(List.of("one", "two"))).detail().isEqualTo("one, two");
    }
}
