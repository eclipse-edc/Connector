/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.assetindex.azure;

import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.assetindex.azure.model.AssetDocument;
import org.eclipse.dataspaceconnector.cosmos.azure.CosmosDbApi;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.matches;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.strictMock;
import static org.easymock.EasyMock.verify;

class CosmosAssetIndexTest {

    private CosmosDbApi api;
    private TypeManager typeManager;
    private RetryPolicy<Object> retryPolicy;

    private static AssetDocument createDocument(String id) {
        return new AssetDocument(Asset.Builder.newInstance().id(id).build(), "partitionkey-test");
    }

    @BeforeEach
    public void setUp() {
        typeManager = new TypeManager();
        typeManager.registerTypes(AssetDocument.class, Asset.class);
        retryPolicy = new RetryPolicy<>().withMaxRetries(1);
        api = strictMock(CosmosDbApi.class);
    }

    @AfterEach
    public void tearDown() {
        reset(api);
    }

    @Test
    void inputValidation() {
        // null cosmos api
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> new CosmosAssetIndex(null, null, retryPolicy));

        // type manager is null
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> new CosmosAssetIndex(api, null, retryPolicy));

        // retry policy is null
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> new CosmosAssetIndex(api, typeManager, null));
    }

    @Test
    void findById() {
        String id = "id-test";
        AssetDocument document = createDocument(id);
        expect(api.queryItemById(eq(id))).andReturn(document);

        replay(api);

        CosmosAssetIndex assetIndex = new CosmosAssetIndex(api, typeManager, retryPolicy);

        Asset actualAsset = assetIndex.findById(id);
        assertThat(actualAsset.getProperties()).isEqualTo(document.getWrappedInstance().getProperties());

        verify(api);
    }

    @Test
    void findByIdThrowEdcException() {
        String id = "id-test";
        expect(api.queryItemById(eq(id)))
                .andThrow(new EdcException("Failed to fetch object"))
                .andThrow(new EdcException("Failed again to find object"));

        replay(api);

        CosmosAssetIndex assetIndex = new CosmosAssetIndex(api, typeManager, retryPolicy);

        assertThatExceptionOfType(EdcException.class).isThrownBy(() -> assetIndex.findById(id));

        verify(api);
    }

    @Test
    void findByIdReturnsNull() {
        String id = "id-test";
        expect(api.queryItemById(eq(id))).andReturn(null);

        replay(api);

        CosmosAssetIndex assetIndex = new CosmosAssetIndex(api, typeManager, retryPolicy);

        Asset actualAsset = assetIndex.findById(id);
        assertThat(actualAsset).isNull();

        verify(api);
    }

    @Test
    void queryAssets() {
        String id1 = UUID.randomUUID().toString();
        String id2 = UUID.randomUUID().toString();
        expect(api.queryItems(anyObject())).andReturn(List.of(createDocument(id1), createDocument(id2)));

        replay(api);

        CosmosAssetIndex assetIndex = new CosmosAssetIndex(api, typeManager, retryPolicy);

        List<Asset> assets = assetIndex.queryAssets(AssetSelectorExpression.SELECT_ALL).collect(Collectors.toList());
        assertThat(assets)
                .anyMatch(asset -> asset.getId().equals(id1))
                .anyMatch(asset -> asset.getId().equals(id2));

        verify(api);
    }

    @Test
    void queryAssets_withSelection() {
        String id1 = UUID.randomUUID().toString();
        String id2 = UUID.randomUUID().toString();
        // let's verify that the query actually contains the proper WHERE clause
        expect(api.queryItems(matches(".*WHERE AssetDocument.*" + Asset.PROPERTY_NAME + " = 'somename'"))).andReturn(List.of(createDocument(id1), createDocument(id2)));

        replay(api);

        CosmosAssetIndex assetIndex = new CosmosAssetIndex(api, typeManager, retryPolicy);

        var selectByName = AssetSelectorExpression.Builder.newInstance().whenEquals(Asset.PROPERTY_NAME, "somename").build();
        List<Asset> assets = assetIndex.queryAssets(selectByName).collect(Collectors.toList());
        assertThat(assets)
                .anyMatch(asset -> asset.getId().equals(id1))
                .anyMatch(asset -> asset.getId().equals(id2));

        verify(api);
    }
}
