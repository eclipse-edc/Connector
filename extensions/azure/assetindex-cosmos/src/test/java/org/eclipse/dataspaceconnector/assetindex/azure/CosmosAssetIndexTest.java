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

import com.azure.cosmos.models.SqlQuerySpec;
import net.jodah.failsafe.RetryPolicy;
import org.easymock.Capture;
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
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.strictMock;
import static org.easymock.EasyMock.verify;

class CosmosAssetIndexTest {

    private static final String TEST_PARTITION_KEY = "test-partition-key";
    private CosmosDbApi api;
    private TypeManager typeManager;
    private RetryPolicy<Object> retryPolicy;

    private static AssetDocument createDocument(String id) {
        return new AssetDocument(Asset.Builder.newInstance().id(id).build(), "partitionkey-test", null);
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
                .isThrownBy(() -> new CosmosAssetIndex(null, TEST_PARTITION_KEY, null, retryPolicy));

        // type manager is null
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> new CosmosAssetIndex(api, TEST_PARTITION_KEY, null, retryPolicy));

        // retry policy is null
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> new CosmosAssetIndex(api, TEST_PARTITION_KEY, typeManager, null));
    }

    @Test
    void findById() {
        String id = "id-test";
        AssetDocument document = createDocument(id);
        expect(api.queryItemById(eq(id))).andReturn(document);

        replay(api);

        CosmosAssetIndex assetIndex = new CosmosAssetIndex(api, TEST_PARTITION_KEY, typeManager, retryPolicy);

        Asset actualAsset = assetIndex.findById(id);
        assertThat(actualAsset.getProperties()).isEqualTo(document.getWrappedAsset().getProperties());

        verify(api);
    }

    @Test
    void findByIdThrowEdcException() {
        String id = "id-test";
        expect(api.queryItemById(eq(id)))
                .andThrow(new EdcException("Failed to fetch object"))
                .andThrow(new EdcException("Failed again to find object"));

        replay(api);

        CosmosAssetIndex assetIndex = new CosmosAssetIndex(api, TEST_PARTITION_KEY, typeManager, retryPolicy);

        assertThatExceptionOfType(EdcException.class).isThrownBy(() -> assetIndex.findById(id));

        verify(api);
    }

    @Test
    void findByIdReturnsNull() {
        String id = "id-test";
        expect(api.queryItemById(eq(id))).andReturn(null);

        replay(api);

        CosmosAssetIndex assetIndex = new CosmosAssetIndex(api, TEST_PARTITION_KEY, typeManager, retryPolicy);

        Asset actualAsset = assetIndex.findById(id);
        assertThat(actualAsset).isNull();

        verify(api);
    }

    @Test
    void queryAssets() {
        String id1 = UUID.randomUUID().toString();
        String id2 = UUID.randomUUID().toString();
        expect(api.queryItems(anyObject(SqlQuerySpec.class))).andReturn(Stream.of(createDocument(id1), createDocument(id2)));

        replay(api);

        CosmosAssetIndex assetIndex = new CosmosAssetIndex(api, TEST_PARTITION_KEY, typeManager, retryPolicy);

        List<Asset> assets = assetIndex.queryAssets(AssetSelectorExpression.SELECT_ALL).collect(Collectors.toList());
        assertThat(assets)
                .anyMatch(asset -> asset.getId().equals(id1))
                .anyMatch(asset -> asset.getId().equals(id2));

        verify(api);
    }

    @Test
    void queryAssets_operatorIn() {
        String id1 = UUID.randomUUID().toString();
        String id2 = UUID.randomUUID().toString();
        Capture<SqlQuerySpec> queryCapture = newCapture();
        expect(api.queryItems(capture(queryCapture))).andReturn(Stream.of(createDocument(id1), createDocument(id2)));

        replay(api);

        CosmosAssetIndex assetIndex = new CosmosAssetIndex(api, TEST_PARTITION_KEY, typeManager, retryPolicy);

        var inExpr = "(" + String.join(",", List.of(id1, id2)) + ")";
        var selector = AssetSelectorExpression.Builder.newInstance()
                .constraint(Asset.PROPERTY_ID, "IN", inExpr)
                .build();
        List<Asset> assets = assetIndex.queryAssets(selector).collect(Collectors.toList());

        assertThat(assets).hasSize(2)
                .anyMatch(asset -> asset.getId().equals(id1))
                .anyMatch(asset -> asset.getId().equals(id2));

        assertThat(queryCapture.hasCaptured()).isTrue();
        assertThat(queryCapture.getValue().getQueryText()).contains("WHERE AssetDocument.wrappedInstance.asset_prop_id IN (" + id1 + "," + id2 + ")");

        verify(api);
    }

    @Test
    void queryAssets_withSelection() {
        String id1 = UUID.randomUUID().toString();
        String id2 = UUID.randomUUID().toString();
        // let's verify that the query actually contains the proper WHERE clause
        Capture<SqlQuerySpec> specCapture = newCapture();
        expect(api.queryItems(capture(specCapture))).andReturn(Stream.of(createDocument(id1), createDocument(id2)));
        replay(api);

        CosmosAssetIndex assetIndex = new CosmosAssetIndex(api, TEST_PARTITION_KEY, typeManager, retryPolicy);

        var selectByName = AssetSelectorExpression.Builder.newInstance().whenEquals(Asset.PROPERTY_NAME, "'somename'").build();
        List<Asset> assets = assetIndex.queryAssets(selectByName).collect(Collectors.toList());
        assertThat(assets)
                .anyMatch(asset -> asset.getId().equals(id1))
                .anyMatch(asset -> asset.getId().equals(id2));

        assertThat(specCapture.hasCaptured()).isTrue();
        assertThat(specCapture.getValue().getQueryText()).matches(".*WHERE AssetDocument.* = 'somename'");

        verify(api);
    }
}
