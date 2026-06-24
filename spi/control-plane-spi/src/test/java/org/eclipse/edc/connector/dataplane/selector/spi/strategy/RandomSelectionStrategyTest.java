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

package org.eclipse.edc.connector.dataplane.selector.spi.strategy;

import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.junit.jupiter.api.RepeatedTest;

import java.util.List;

import static java.util.stream.IntStream.range;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class RandomSelectionStrategyTest {

    private static final List<DataPlaneInstance> INSTANCES = range(0, 1000)
            .mapToObj(it -> DataPlaneInstance.Builder.newInstance().url("http://any/" + it).build())
            .toList();

    private final RandomSelectionStrategy strategy = new RandomSelectionStrategy();

    // Repeat this test many times to ensure we're always getting a non-null result
    @RepeatedTest(10000)
    void verifyNonNull() {
        assertThat(strategy.apply(INSTANCES)).isNotNull().isIn(INSTANCES);
    }
}
