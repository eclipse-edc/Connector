/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.edr.spi.store;

import org.eclipse.edc.edr.spi.TestFunctions;
import org.eclipse.edc.junit.assertions.AbstractResultAssert;
import org.eclipse.edc.spi.result.StoreFailure;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.Test;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Default test suite for {@link EndpointDataReferenceCache} implementors
 */
public abstract class EndpointDataReferenceCacheTestBase {

    @Test
    void put() {
        var tpId = "tp1";
        var assetId = "asset1";
        var dataAddress = dataAddress();
        var entry = TestFunctions.edrEntry(assetId, randomUUID().toString(), tpId, randomUUID().toString());

        getCache().put(entry.getTransferProcessId(), dataAddress);

        AbstractResultAssert.assertThat(getCache().get(tpId))
                .isSucceeded()
                .usingRecursiveComparison()
                .isEqualTo(dataAddress);

    }

    @Test
    void delete_shouldDelete_WhenFound() {

        var entry = TestFunctions.edrEntry("assetId", "agreementId", "tpId", "cnId");
        getCache().put(entry.getTransferProcessId(), DataAddress.Builder.newInstance().type("test").build());

        AbstractResultAssert.assertThat(getCache().delete(entry.getTransferProcessId())).isSucceeded();

        AbstractResultAssert.assertThat(getCache().get("tpId")).isFailed();

    }

    @Test
    void delete_shouldReturnError_whenNotFound() {
        assertThat(getCache().delete("notFound"))
                .extracting(StoreResult::reason)
                .isEqualTo(StoreFailure.Reason.GENERAL_ERROR);
    }

    protected abstract EndpointDataReferenceCache getCache();

    private DataAddress dataAddress() {
        return DataAddress.Builder.newInstance().type("test").build();
    }


}
