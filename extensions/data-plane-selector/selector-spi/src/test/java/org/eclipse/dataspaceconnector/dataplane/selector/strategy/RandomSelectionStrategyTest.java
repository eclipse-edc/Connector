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

package org.eclipse.dataspaceconnector.dataplane.selector.strategy;

import org.eclipse.dataspaceconnector.dataplane.selector.instance.DataPlaneInstance;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;

class RandomSelectionStrategyTest {
    private static List<DataPlaneInstance> instances;
    private RandomSelectionStrategy strategy;

    @BeforeAll
    static void prepare() {
        instances = Collections.nCopies(1000, mock(DataPlaneInstance.class));
    }

    @BeforeEach
    void setUp() {
        strategy = new RandomSelectionStrategy();

    }

    // Repeat this test many times to ensure we're always getting a non-null result
    @RepeatedTest(10000)
    void verifyNonNull() {
        assertThat(strategy.apply(instances)).isNotNull().isIn(instances);
    }
}