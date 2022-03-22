/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.store.memory;

import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.dataspaceconnector.transfer.store.memory.TestFunctions.createProcess;

class TransferProcessPredicateConverterTest {
    private TransferProcessPredicateConverter converter;

    @BeforeEach
    void setUp() {
        converter = new TransferProcessPredicateConverter();
    }

    @Test
    void convert_nameEquals() {
        var criterion = new Criterion("id", "=", "test-cn");
        var n = createProcess("test-cn");
        var predicate = converter.convert(criterion);

        assertThat(predicate).isNotNull();
        assertThat(predicate.test(n)).isTrue();
    }

    @Test
    void convert_operatorIn() {
        var n = createProcess("test-cn");
        var criterion = new Criterion("id", "in", "(bob, test-cn)");
        var pred = converter.convert(criterion);
        assertThat(pred).isNotNull().accepts(n);

    }

    @Test
    void convert_invalidOperator() {
        var criterion = new Criterion("name", "GREATER_THAN", "(bob, alice)");
        assertThatThrownBy(() -> converter.convert(criterion)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Operator [GREATER_THAN] is not supported by this converter!");

    }
}