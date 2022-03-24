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
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class RandomSelectionStrategyTest {
    private static List<DataPlaneInstance> instances;
    private RandomSelectionStrategy strategy;

    @BeforeAll
    static void prepare() {
        instances = IntStream.range(0, 1000).mapToObj(RandomSelectionStrategyTest::createInstance).collect(Collectors.toList());
    }

    private static DataPlaneInstance createInstance(int i) {
        return new DataPlaneInstance() {
            @Override
            public String getId() {
                return null;
            }

            @Override
            public boolean canHandle(DataAddress sourceAddress, DataAddress destinationAddress) {
                return false;
            }

            @Override
            public URL getUrl() {
                return null;
            }

            @Override
            public int getTurnCount() {
                return 0;
            }

            @Override
            public long getLastActive() {
                return 0;
            }

            @Override
            public Map<String, Object> getProperties() {
                return null;
            }
        };
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