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

package org.eclipse.dataspaceconnector.common.statemachine;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StateProcessorImplTest {

    @Test
    void shouldReturnTheProcessedCount() {
        var processor = new StateProcessorImpl<>(() -> List.of("any"), string -> true);

        var count = processor.process();

        assertThat(count).isEqualTo(1);
    }

    @Test
    void shouldNotCountUnprocessedEntities() {
        var processor = new StateProcessorImpl<>(() -> List.of("any"), string -> false);

        var count = processor.process();

        assertThat(count).isEqualTo(0);
    }
}
