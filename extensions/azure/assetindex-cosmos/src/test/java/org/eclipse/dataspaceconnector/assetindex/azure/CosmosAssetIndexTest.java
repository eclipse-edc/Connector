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

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.implementation.NotFoundException;
import com.azure.cosmos.models.CosmosItemResponse;
import net.jodah.failsafe.RetryPolicy;
import org.eclipse.dataspaceconnector.assetindex.azure.model.AssetDocument;
import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.niceMock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.strictMock;
import static org.easymock.EasyMock.verify;

class CosmosAssetIndexTest {

    private TypeManager typeManager;
    private RetryPolicy<Object> retryPolicy;

    @BeforeEach
    public void setUp() {
        typeManager = new TypeManager();
        typeManager.registerTypes(AssetDocument.class, Asset.class);
        retryPolicy = new RetryPolicy<>().withMaxRetries(1);
    }

    @Test
    void inputValidation() {
        CosmosContainer container = niceMock(CosmosContainer.class);

        // null cosmos container
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> new CosmosAssetIndex(null, null, typeManager, retryPolicy, false));

        // partition key null or empty
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> new CosmosAssetIndex(container, null, typeManager, retryPolicy, false));
        assertThatExceptionOfType(EdcException.class)
                .isThrownBy(() -> new CosmosAssetIndex(container, "", typeManager, retryPolicy, false));

        // type manager is null
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> new CosmosAssetIndex(container, "partition-key", null, retryPolicy, false));

        // retry policy is null
        assertThatExceptionOfType(NullPointerException.class)
                .isThrownBy(() -> new CosmosAssetIndex(container, "partition-key", typeManager, null, false));
    }


    @Test
    void findById() {
        CosmosItemResponse<Object> response = strictMock(CosmosItemResponse.class);
        CosmosContainer container = strictMock(CosmosContainer.class);

        String id = "id-test";
        AssetDocument document = createDocument(id);
        expect(response.getItem()).andReturn(document);

        expect(container.readItem(eq(id), anyObject(), anyObject(), anyObject())).andReturn(response);

        replay(container, response);

        CosmosAssetIndex assetIndex = new CosmosAssetIndex(container, "partitionkey-test", typeManager, retryPolicy, false);

        Asset actualAsset = assetIndex.findById(id);

        assertThat(actualAsset.getProperties()).isEqualTo(document.getWrappedInstance().getProperties());

        verify(container, response);
    }

    @Test
    void findById_notFound() {
        CosmosContainer container = niceMock(CosmosContainer.class);

        String id = "id-test";

        expect(container.readItem(eq(id), anyObject(), anyObject(), anyObject()))
                .andThrow(new NotFoundException())
                .andThrow(new NotFoundException());

        replay(container);

        CosmosAssetIndex assetIndex = new CosmosAssetIndex(container, "partitionkey-test", typeManager, retryPolicy, false);

        Asset actualAsset = assetIndex.findById(id);

        assertThat(actualAsset).isNull();

        verify(container);
    }

    private static AssetDocument createDocument(String id) {
        return new AssetDocument(Asset.Builder.newInstance()
                .id(id)
                .name("node-test")
                .contentType("application/json")
                .version("123")
                .property("hello", "world")
                .property("foo", "bar")
                .build(),
                "partitionkey-test");
    }
}
