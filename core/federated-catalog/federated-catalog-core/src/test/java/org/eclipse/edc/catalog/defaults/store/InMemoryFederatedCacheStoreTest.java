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

package org.eclipse.edc.catalog.defaults.store;


import org.eclipse.edc.catalog.store.InMemoryFederatedCacheStore;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.query.CriterionConverter;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.util.concurrency.LockManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryFederatedCacheStoreTest {

    private InMemoryFederatedCacheStore store;

    private static Asset createAsset(String id) {
        return Asset.Builder.newInstance()
                .id(id)
                .build();
    }

    private static ContractOffer createContractOffer(String id, Asset asset) {
        return ContractOffer.Builder.newInstance()
                .id(id)
                .asset(asset)
                .policy(Policy.Builder.newInstance().build())
                .build();
    }

    @BeforeEach
    public void setUp() {
        CriterionConverter<Predicate<ContractOffer>> converter = criterion -> offer -> true;
        store = new InMemoryFederatedCacheStore(converter, new LockManager(new ReentrantReadWriteLock()));
    }

    @Test
    void queryCacheContainingOneElementWithNoCriterion_shouldReturnUniqueElement() {
        var contractOfferId = UUID.randomUUID().toString();
        var assetId = UUID.randomUUID().toString();
        var contractOffer = createContractOffer(contractOfferId, createAsset(assetId));

        store.save(contractOffer);

        Collection<ContractOffer> result = store.query(Collections.emptyList());

        assertThat(result)
                .hasSize(1)
                .allSatisfy(co -> assertThat(co.getAsset().getId()).isEqualTo(assetId));
    }

    @Test
    void queryCacheAfterInsertingSameAssetTwice_shouldReturnLastInsertedContractOfferOnly() {
        var contractOfferId1 = UUID.randomUUID().toString();
        var contractOfferId2 = UUID.randomUUID().toString();
        var assetId = UUID.randomUUID().toString();
        var contractOffer1 = createContractOffer(contractOfferId1, createAsset(assetId));
        var contractOffer2 = createContractOffer(contractOfferId2, createAsset(assetId));

        store.save(contractOffer1);
        store.save(contractOffer2);

        Collection<ContractOffer> result = store.query(Collections.emptyList());

        assertThat(result)
                .hasSize(1)
                .allSatisfy(co -> {
                    assertThat(co.getId()).isEqualTo(contractOfferId2);
                    assertThat(co.getAsset().getId()).isEqualTo(assetId);
                });
    }

    @Test
    void queryCacheContainingTwoDistinctAssets_shouldReturnBothContractOffers() {
        var contractOfferId1 = UUID.randomUUID().toString();
        var contractOfferId2 = UUID.randomUUID().toString();
        var assetId1 = UUID.randomUUID().toString();
        var assetId2 = UUID.randomUUID().toString();
        var contractOffer1 = createContractOffer(contractOfferId1, createAsset(assetId1));
        var contractOffer2 = createContractOffer(contractOfferId2, createAsset(assetId2));

        store.save(contractOffer1);
        store.save(contractOffer2);

        Collection<ContractOffer> result = store.query(Collections.emptyList());

        assertThat(result)
                .hasSize(2)
                .anySatisfy(co -> assertThat(co.getAsset().getId()).isEqualTo(assetId1))
                .anySatisfy(co -> assertThat(co.getAsset().getId()).isEqualTo(assetId2));
    }

    @Test
    void removedMarked_noneMarked() {
        var contractOfferId1 = UUID.randomUUID().toString();
        var contractOfferId2 = UUID.randomUUID().toString();
        var assetId1 = UUID.randomUUID().toString();
        var assetId2 = UUID.randomUUID().toString();
        var contractOffer1 = createContractOffer(contractOfferId1, createAsset(assetId1));
        var contractOffer2 = createContractOffer(contractOfferId2, createAsset(assetId2));

        store.save(contractOffer1);
        store.save(contractOffer2);

        assertThat(store.query(List.of())).hasSize(2);

        store.deleteExpired(); // none of them is marked, d
        assertThat(store.query(List.of())).containsExactlyInAnyOrder(contractOffer1, contractOffer2);

    }

    @Test
    void removedMarked_shouldDeleteMarked() {
        var contractOfferId1 = UUID.randomUUID().toString();
        var contractOfferId2 = UUID.randomUUID().toString();
        var assetId1 = UUID.randomUUID().toString();
        var assetId2 = UUID.randomUUID().toString();
        var contractOffer1 = createContractOffer(contractOfferId1, createAsset(assetId1));
        var contractOffer2 = createContractOffer(contractOfferId2, createAsset(assetId2));

        store.save(contractOffer1);
        store.save(contractOffer2);

        assertThat(store.query(List.of())).hasSize(2);

        store.expireAll(); // two items marked
        store.save(createContractOffer(UUID.randomUUID().toString(), createAsset(UUID.randomUUID().toString())));
        store.deleteExpired(); // should delete only marked items
        assertThat(store.query(List.of())).hasSize(1)
                .doesNotContain(contractOffer1, contractOffer2);

    }
}