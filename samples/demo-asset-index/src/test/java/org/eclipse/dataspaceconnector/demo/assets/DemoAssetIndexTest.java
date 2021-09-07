/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.demo.assets;

import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

public class DemoAssetIndexTest {

    private static final AssetSelectorExpression SELECT_ALL = AssetSelectorExpression.builder().build();

    private AssetIndex fixtureAssetIndex;

    @BeforeEach
    public void beforeEach() {
        fixtureAssetIndex = new DemoAssetIndex();
    }

    @Test
    public void testIndexReturnsAllFixtures() {
        final Stream<Asset> assets = fixtureAssetIndex.queryAssets(SELECT_ALL);
        Assertions.assertEquals(DemoFixtures.FIXTURES.length, assets.count());
    }
}
