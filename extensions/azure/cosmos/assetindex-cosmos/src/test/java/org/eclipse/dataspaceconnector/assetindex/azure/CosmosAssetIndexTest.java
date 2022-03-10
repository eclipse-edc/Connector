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

import com.azure.cosmos.implementation.NotFoundException;
import com.azure.cosmos.models.SqlQuerySpec;
import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.assetindex.azure.model.AssetDocument;
import org.eclipse.dataspaceconnector.azure.cosmos.CosmosDbApi;
import org.eclipse.dataspaceconnector.common.matchers.PredicateMatcher;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.query.SortOrder;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class CosmosAssetIndexTest {

    private static final String TEST_PARTITION_KEY = "test-partition-key";
    private CosmosDbApi api;
    private TypeManager typeManager;
    private RetryPolicy<Object> retryPolicy;
    private CosmosAssetIndex assetIndex;

    private static AssetDocument createDocument(String id) {
        return new AssetDocument(Asset.Builder.newInstance().id(id).build(), "partitionkey-test", null);
    }

    @BeforeEach
    public void setUp() {
        typeManager = new TypeManager();
        typeManager.registerTypes(AssetDocument.class, Asset.class);
        retryPolicy = new RetryPolicy<>().withMaxRetries(1);
        api = mock(CosmosDbApi.class);
        assetIndex = new CosmosAssetIndex(api, TEST_PARTITION_KEY, typeManager, retryPolicy);
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
        when(api.queryItemById(eq(id))).thenReturn(document);

        Asset actualAsset = assetIndex.findById(id);

        assertThat(actualAsset.getProperties()).isEqualTo(document.getWrappedAsset().getProperties());
        verify(api).queryItemById(eq(id));
    }

    @Test
    void findByIdThrowEdcException() {
        String id = "id-test";
        when(api.queryItemById(eq(id)))
                .thenThrow(new EdcException("Failed to fetch object"))
                .thenThrow(new EdcException("Failed again to find object"));

        assertThatExceptionOfType(EdcException.class).isThrownBy(() -> assetIndex.findById(id));
        verify(api, atLeastOnce()).queryItemById(eq(id));
        verifyNoMoreInteractions(api);
    }

    @Test
    void findByIdReturnsNull() {
        String id = "id-test";
        when(api.queryItemById(eq(id))).thenReturn(null);

        Asset actualAsset = assetIndex.findById(id);

        assertThat(actualAsset).isNull();
        verify(api).queryItemById(eq(id));
        verifyNoMoreInteractions(api);
    }

    @Test
    void queryAssets() {
        String id1 = UUID.randomUUID().toString();
        String id2 = UUID.randomUUID().toString();
        when(api.queryItems(any(SqlQuerySpec.class))).thenReturn(Stream.of(createDocument(id1), createDocument(id2)));

        List<Asset> assets = assetIndex.queryAssets(AssetSelectorExpression.SELECT_ALL).collect(Collectors.toList());

        assertThat(assets)
                .anyMatch(asset -> asset.getId().equals(id1))
                .anyMatch(asset -> asset.getId().equals(id2));
        verify(api).queryItems(any(SqlQuerySpec.class));
        verifyNoMoreInteractions(api);
    }

    @Test
    void queryAssets_operatorIn() {
        String id1 = UUID.randomUUID().toString();
        String id2 = UUID.randomUUID().toString();
        var queryCapture = ArgumentCaptor.forClass(SqlQuerySpec.class);
        when(api.queryItems(queryCapture.capture())).thenReturn(Stream.of(createDocument(id1), createDocument(id2)));

        var inExpr = "(" + String.join(",", List.of(id1, id2)) + ")";
        var selector = AssetSelectorExpression.Builder.newInstance()
                .constraint(Asset.PROPERTY_ID, "IN", inExpr)
                .build();

        var assets = assetIndex.queryAssets(selector);

        assertThat(assets).hasSize(2)
                .anyMatch(asset -> asset.getId().equals(id1))
                .anyMatch(asset -> asset.getId().equals(id2));
        assertThat(queryCapture.getValue().getQueryText()).contains("WHERE AssetDocument.wrappedInstance.asset_prop_id IN (" + id1 + "," + id2 + ")");
        verify(api).queryItems(queryCapture.capture());
        verifyNoMoreInteractions(api);
    }

    @Test
    void queryAssets_withSelection() {
        String id1 = UUID.randomUUID().toString();
        String id2 = UUID.randomUUID().toString();
        // let's verify that the query actually contains the proper WHERE clause
        var queryCapture = ArgumentCaptor.forClass(SqlQuerySpec.class);
        when(api.queryItems(queryCapture.capture())).thenReturn(Stream.of(createDocument(id1), createDocument(id2)));
        CosmosAssetIndex assetIndex = new CosmosAssetIndex(api, TEST_PARTITION_KEY, typeManager, retryPolicy);
        var selectByName = AssetSelectorExpression.Builder.newInstance().whenEquals(Asset.PROPERTY_NAME, "somename").build();

        var assets = assetIndex.queryAssets(selectByName);

        assertThat(assets)
                .anyMatch(asset -> asset.getId().equals(id1))
                .anyMatch(asset -> asset.getId().equals(id2));
        assertThat(queryCapture.getValue().getQueryText()).matches(".*WHERE AssetDocument.* = @asset_prop_name");
        verify(api).queryItems(queryCapture.capture());
        verifyNoMoreInteractions(api);
    }

    @Test
    void findAll_noQuerySpec() {
        String id = "id-test";
        AssetDocument document = createDocument(id);
        var expectedQuery = "SELECT * FROM AssetDocument OFFSET 0 LIMIT 50";
        when(api.queryItems(argThat(queryMatches(expectedQuery)))).thenReturn(Stream.of(document));

        List<Asset> assets = assetIndex.queryAssets(QuerySpec.none()).collect(Collectors.toList());

        assertThat(assets).hasSize(1).extracting(Asset::getId).containsExactly(document.getWrappedAsset().getId());
        assertThat(assets).extracting(Asset::getProperties).allSatisfy(m -> assertThat(m).containsAllEntriesOf(document.getWrappedAsset().getProperties()));
        verify(api).queryItems(any(SqlQuerySpec.class));
    }

    @Test
    void findAll_withPaging_SortingDesc() {
        String id = "id-test";
        AssetDocument document = createDocument(id);
        var expectedQuery = "SELECT * FROM AssetDocument ORDER BY AssetDocument.wrappedInstance.anyField DESC OFFSET 5 LIMIT 100";
        when(api.queryItems(argThat(queryMatches(expectedQuery)))).thenReturn(Stream.of(document));

        List<Asset> assets = assetIndex.queryAssets(QuerySpec.Builder.newInstance()
                        .offset(5)
                        .limit(100)
                        .sortField("anyField")
                        .sortOrder(SortOrder.DESC)
                        .build())
                .collect(Collectors.toList());

        assertThat(assets).hasSize(1).extracting(Asset::getId).containsExactly(document.getWrappedAsset().getId());
        assertThat(assets).extracting(Asset::getProperties).allSatisfy(m -> assertThat(m).containsAllEntriesOf(document.getWrappedAsset().getProperties()));
        verify(api).queryItems(any(SqlQuerySpec.class));
    }

    @Test
    void findAll_withPaging_SortingAsc() {
        String id = "id-test";
        AssetDocument document = createDocument(id);
        var expectedQuery = "SELECT * FROM AssetDocument ORDER BY AssetDocument.wrappedInstance.anyField ASC OFFSET 5 LIMIT 100";
        when(api.queryItems(argThat(queryMatches(expectedQuery)))).thenReturn(Stream.of(document));

        List<Asset> assets = assetIndex.queryAssets(QuerySpec.Builder.newInstance()
                        .offset(5)
                        .limit(100)
                        .sortField("anyField")
                        .sortOrder(SortOrder.ASC)
                        .build())
                .collect(Collectors.toList());

        assertThat(assets).hasSize(1).extracting(Asset::getId).containsExactly(document.getWrappedAsset().getId());
        assertThat(assets).extracting(Asset::getProperties).allSatisfy(m -> assertThat(m).containsAllEntriesOf(document.getWrappedAsset().getProperties()));
        verify(api).queryItems(any(SqlQuerySpec.class));
    }

    @Test
    void findAll_withFiltering() {
        String id = "id-test";
        AssetDocument document = createDocument(id);
        var expectedQuery = "SELECT * FROM AssetDocument WHERE AssetDocument.wrappedInstance.someField = @someField OFFSET 5 LIMIT 100";
        when(api.queryItems(argThat(queryMatches(expectedQuery)))).thenReturn(Stream.of(document));

        List<Asset> assets = assetIndex.queryAssets(QuerySpec.Builder.newInstance()
                        .offset(5)
                        .limit(100)
                        .filter("someField=randomValue")
                        .build())
                .collect(Collectors.toList());

        assertThat(assets).hasSize(1).extracting(Asset::getId).containsExactly(document.getWrappedAsset().getId());
        assertThat(assets).extracting(Asset::getProperties).allSatisfy(m -> assertThat(m).containsAllEntriesOf(document.getWrappedAsset().getProperties()));
        verify(api).queryItems(any(SqlQuerySpec.class));
    }

    @Test
    void deleteById_whenPresent_deletesItem() {
        String id = "id-test";
        AssetDocument document = createDocument(id);
        when(api.deleteItem(id)).thenReturn(document);

        var deletedAsset = assetIndex.deleteById(id);
        assertThat(deletedAsset.getProperties()).isEqualTo(document.getWrappedAsset().getProperties());
        verify(api).deleteItem(eq(id));
    }

    @Test
    void deleteById_whenAlreadyMissing_returnsNull() {
        String id = "id-test";
        when(api.deleteItem(eq(id))).thenThrow(new NotFoundException());
        assertThat(assetIndex.deleteById(id)).isNull();
    }

    @NotNull
    private PredicateMatcher<SqlQuerySpec> queryMatches(String expectedQuery) {
        return new PredicateMatcher<>(sqlQuerySpec -> sqlQuerySpec.getQueryText().equals(expectedQuery));
    }

}
