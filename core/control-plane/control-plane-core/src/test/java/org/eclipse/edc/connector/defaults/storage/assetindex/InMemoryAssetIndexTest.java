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

package org.eclipse.edc.connector.defaults.storage.assetindex;


import org.eclipse.edc.connector.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.query.asset.AssetPropertyLookup;
import org.eclipse.edc.query.CriterionOperatorRegistryImpl;
import org.eclipse.edc.spi.testfixtures.asset.AssetIndexTestBase;
import org.junit.jupiter.api.BeforeEach;

class InMemoryAssetIndexTest extends AssetIndexTestBase {

    private InMemoryAssetIndex index;

    @BeforeEach
    void setUp() {
        var registry = CriterionOperatorRegistryImpl.ofDefaults();
        registry.registerPropertyLookup(new AssetPropertyLookup());
        index = new InMemoryAssetIndex(registry);
    }

    @Override
    protected AssetIndex getAssetIndex() {
        return index;
    }

}
