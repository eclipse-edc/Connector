/*
 *  Copyright (c) 2021 Microsoft Corporation
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
package org.eclipse.dataspaceconnector.catalog.directory;

import org.eclipse.dataspaceconnector.catalog.spi.QueryAdapter;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.strictMock;
import static org.easymock.EasyMock.verify;


class InMemoryQueryAdapterRegistryTest {
    private static final Asset ASSET_ABC = Asset.Builder.newInstance().id("ABC").build();
    private static final Asset ASSET_DEF = Asset.Builder.newInstance().id("DEF").build();
    private static final Asset ASSET_XYZ = Asset.Builder.newInstance().id("XYZ").build();

    private InMemoryQueryAdapterRegistry registry;

    @Test
    void getAllAdapters() {
        // initialize
        QueryAdapter adapter1 = strictMock(QueryAdapter.class);
        QueryAdapter adapter2 = strictMock(QueryAdapter.class);
        QueryAdapter adapter3 = strictMock(QueryAdapter.class);

        expect(adapter1.executeQuery(anyObject())).andReturn(Stream.of(ASSET_ABC));
        expect(adapter2.executeQuery(anyObject())).andReturn(Stream.of(ASSET_DEF));
        expect(adapter3.executeQuery(anyObject())).andReturn(Stream.of(ASSET_XYZ));

        registry.register(adapter1);
        registry.register(adapter2);
        registry.register(adapter3);

        replay(adapter1, adapter2, adapter3);

        // start testing
        Collection<QueryAdapter> adapters = registry.getAllAdapters();

        assertThat(collectAssetsFromAdapters(adapters)).isEqualTo(Arrays.asList(ASSET_ABC, ASSET_DEF, ASSET_XYZ));

        verify(adapter1, adapter2, adapter3);
    }

    @BeforeEach
    public void setUp() {
        this.registry = new InMemoryQueryAdapterRegistry();
    }

    private List<Asset> collectAssetsFromAdapters(Collection<QueryAdapter> adapters) {
        List<Asset> assets = new ArrayList<>();
        adapters.forEach(queryAdapter -> assets.addAll(queryAdapter.executeQuery(null).collect(Collectors.toList())));
        return assets;
    }
}
