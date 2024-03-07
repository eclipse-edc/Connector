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

package org.eclipse.tractusx.edc.edr.spi.store;

import org.eclipse.edc.edr.spi.store.EndpointDataReferenceEntryIndex;
import org.eclipse.edc.edr.spi.types.EndpointDataReferenceEntry;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreFailure;
import org.eclipse.edc.spi.result.StoreResult;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tractusx.edc.edr.spi.TestFunctions.edrEntry;

/**
 * Default test suite for {@link EndpointDataReferenceEntryIndex} implementors
 */
public abstract class EndpointDataReferenceEntryIndexTestBase {

    @Test
    void save() {

        var tpId = "tp1";
        var assetId = "asset1";

        var entry = edrEntry(assetId, randomUUID().toString(), tpId, randomUUID().toString());

        getStore().save(entry);

        var results = getStore().query(QuerySpec.max());

        assertThat(results.succeeded()).isTrue();
        assertThat(results.getContent()).hasSize(1)
                .first()
                .isNotNull()
                .extracting(EndpointDataReferenceEntry::getTransferProcessId)
                .isEqualTo(tpId);

    }

    @Test
    void query_noQuerySpec() {
        var all = IntStream.range(0, 10)
                .mapToObj(i -> edrEntry("assetId" + i, "agreementId" + i, "tpId" + i, "cnId" + i))
                .peek(entry -> getStore().save(entry))
                .collect(Collectors.toList());

        var results = getStore().query(QuerySpec.max());

        assertThat(results.succeeded()).isTrue();
        assertThat(results.getContent()).containsExactlyInAnyOrderElementsOf(all);

    }

    @Test
    void query_assetIdQuerySpec() {
        IntStream.range(0, 10)
                .mapToObj(i -> edrEntry("assetId" + i, "agreementId" + i, "tpId" + i, "cnId" + i))
                .forEach(entry -> getStore().save(entry));

        var entry = edrEntry("assetId", "agreementId", "tpId", "cnId");
        getStore().save(entry);

        var filter = Criterion.Builder.newInstance()
                .operandLeft("assetId")
                .operator("=")
                .operandRight(entry.getAssetId())
                .build();

        var results = getStore().query(QuerySpec.Builder.newInstance().filter(filter).build());

        assertThat(results.succeeded()).isTrue();
        assertThat(results.getContent()).containsOnly(entry);

    }

    @Test
    void query_agreementIdQuerySpec() {
        IntStream.range(0, 10)
                .mapToObj(i -> edrEntry("assetId" + i, "agreementId" + i, "tpId" + i, "cnId" + i))
                .forEach(entry -> getStore().save(entry));


        var entry = edrEntry("assetId", "agreementId", "tpId", "cnId");
        getStore().save(entry);

        var filter = Criterion.Builder.newInstance()
                .operandLeft("agreementId")
                .operator("=")
                .operandRight(entry.getAgreementId())
                .build();

        var results = getStore().query(QuerySpec.Builder.newInstance().filter(filter).build());

        assertThat(results.succeeded()).isTrue();
        assertThat(results.getContent()).containsOnly(entry);

    }
    
    @Test
    void delete_shouldDelete_WhenFound() {

        var entry = edrEntry("assetId", "agreementId", "tpId", "cnId");
        getStore().save(entry);

        assertThat(getStore().delete(entry.getTransferProcessId()))
                .extracting(StoreResult::getContent)
                .isEqualTo(entry);

        var results = getStore().query(QuerySpec.max());

        assertThat(results.succeeded()).isTrue();
        assertThat(results.getContent()).hasSize(0);

    }

    @Test
    void deleteX_shouldReturnError_whenNotFound() {
        assertThat(getStore().delete("notFound"))
                .extracting(StoreResult::reason)
                .isEqualTo(StoreFailure.Reason.NOT_FOUND);
    }

    protected abstract EndpointDataReferenceEntryIndex getStore();

}
