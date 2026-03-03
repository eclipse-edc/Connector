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

package org.eclipse.edc.catalog.spi.testfixtures;

import org.eclipse.edc.catalog.spi.CatalogConstants;
import org.eclipse.edc.catalog.spi.FederatedCatalogCache;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.catalog.spi.Catalog;
import org.eclipse.edc.connector.controlplane.catalog.spi.DataService;
import org.eclipse.edc.connector.controlplane.catalog.spi.Dataset;
import org.eclipse.edc.connector.controlplane.catalog.spi.Distribution;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class FederatedCatalogCacheTestBase {

    protected abstract FederatedCatalogCache getStore();

    private Catalog createCatalog(String id, Asset asset) {
        return createCatalogBuilder(id, asset).build();
    }

    private Catalog.Builder createCatalogBuilder(String id, Asset asset) {
        return createCatalogBuilder(id, asset, null);
    }

    private Catalog.Builder createCatalogBuilder(String id, Asset asset, String ednpointUrl) {
        var dataService = DataService.Builder.newInstance().endpointUrl(ednpointUrl).build();
        var dataset = Dataset.Builder.newInstance().id(asset.getId()).properties(asset.getProperties()).distributions(List.of(Distribution.Builder.newInstance().dataService(dataService).format("test-format").build())).build();

        var nestedCatalog = Catalog.Builder.newInstance().participantId("participantId").build();

        return Catalog.Builder.newInstance()
                .id(id)
                .dataServices(List.of(dataService))
                .datasets(List.of(dataset, nestedCatalog))
                .property(CatalogConstants.PROPERTY_ORIGINATOR, "https://test.source/" + id);
    }

    private Asset createAsset(String id) {
        return createAssetBuilder(id)
                .build();
    }

    private Asset.Builder createAssetBuilder(String id) {
        return Asset.Builder.newInstance()
                .id(id);
    }

    @Nested
    class Save {

        @Test
        void save_shouldReturnUniqueElement() {
            var contractOfferId = UUID.randomUUID().toString();
            var assetId = UUID.randomUUID().toString();
            var catalogEntry = createCatalog(contractOfferId, createAsset(assetId));

            getStore().save(catalogEntry);

            var result = getStore().query(QuerySpec.none());

            assertThat(result)
                    .hasSize(1)
                    .allSatisfy(co -> {
                        assertThat(co.getDatasets().get(0).getId()).isEqualTo(assetId);
                        assertThat(co.getDatasets().get(1)).isInstanceOf(Catalog.class);
                    });
        }

        @Test
        void save_shouldReturnLastInsertedContractOfferOnly() {
            var contractOfferId1 = UUID.randomUUID().toString();
            var assetId = UUID.randomUUID().toString();
            var entry1 = createCatalog(contractOfferId1, createAsset(assetId));
            var entry2 = createCatalog(contractOfferId1, createAsset(assetId));

            getStore().save(entry1);
            getStore().save(entry2);

            var result = getStore().query(QuerySpec.none());

            assertThat(result)
                    .hasSize(1)
                    .allSatisfy(co -> {
                        assertThat(co.getId()).isEqualTo(contractOfferId1);
                        assertThat(co.getDatasets().get(0).getId()).isEqualTo(assetId);
                    });
        }

        @Test
        void save_shouldReturnBothContractOffers() {
            var contractOfferId1 = UUID.randomUUID().toString();
            var contractOfferId2 = UUID.randomUUID().toString();
            var assetId1 = UUID.randomUUID().toString();
            var assetId2 = UUID.randomUUID().toString();
            var entry1 = createCatalog(contractOfferId1, createAsset(assetId1));
            var entry2 = createCatalog(contractOfferId2, createAsset(assetId2));

            getStore().save(entry1);
            getStore().save(entry2);

            var result = getStore().query(QuerySpec.none());

            assertThat(result)
                    .hasSize(2)
                    .anySatisfy(co -> assertThat(co.getDatasets().get(0).getId()).isEqualTo(assetId1))
                    .anySatisfy(co -> assertThat(co.getDatasets().get(0).getId()).isEqualTo(assetId2));
        }
    }

    @Nested
    class Query {

        @Test
        void queryByParticipantId() {
            var contractOfferId1 = UUID.randomUUID().toString();
            var contractOfferId2 = UUID.randomUUID().toString();
            var assetId1 = UUID.randomUUID().toString();
            var assetId2 = UUID.randomUUID().toString();
            var entry1 = createCatalogBuilder(contractOfferId1, createAsset(assetId1)).participantId("participant1").build();
            var entry2 = createCatalogBuilder(contractOfferId2, createAsset(assetId2)).participantId("participant2").build();

            getStore().save(entry1);
            getStore().save(entry2);

            var query = QuerySpec.Builder.newInstance().filter(Criterion.criterion("participantId", "=", entry1.getParticipantId())).build();
            var result = getStore().query(query);

            assertThat(result)
                    .hasSize(1)
                    .allSatisfy(co -> assertThat(co.getParticipantId()).isEqualTo(entry1.getParticipantId()));
        }

        @Test
        void queryByDatasetId() {
            var contractOfferId1 = UUID.randomUUID().toString();
            var contractOfferId2 = UUID.randomUUID().toString();
            var assetId1 = UUID.randomUUID().toString();
            var assetId2 = UUID.randomUUID().toString();
            var entry1 = createCatalogBuilder(contractOfferId1, createAsset(assetId1)).build();
            var entry2 = createCatalogBuilder(contractOfferId2, createAsset(assetId2)).build();

            getStore().save(entry1);
            getStore().save(entry2);

            var query = QuerySpec.Builder.newInstance().filter(Criterion.criterion("datasets.id", "=", assetId2)).build();
            var result = getStore().query(query);

            assertThat(result)
                    .hasSize(1)
                    .allSatisfy(co -> assertThat(co.getDatasets().get(0).getId()).isEqualTo(assetId2));
        }

        @Test
        void queryByCatalogProperty() {
            var contractOfferId1 = UUID.randomUUID().toString();
            var contractOfferId2 = UUID.randomUUID().toString();
            var assetId1 = UUID.randomUUID().toString();
            var assetId2 = UUID.randomUUID().toString();
            var entry1 = createCatalogBuilder(contractOfferId1, createAsset(assetId1)).property("name", "value").build();
            var entry2 = createCatalogBuilder(contractOfferId2, createAsset(assetId2)).build();

            getStore().save(entry1);
            getStore().save(entry2);

            var query = QuerySpec.Builder.newInstance().filter(Criterion.criterion("properties.name", "=", "value")).build();
            var result = getStore().query(query);

            assertThat(result)
                    .hasSize(1)
                    .allSatisfy(co -> assertThat(co.getProperties().get("name")).isEqualTo("value"));
        }

        @Test
        void queryByDataServiceEndpoint() {
            var contractOfferId1 = UUID.randomUUID().toString();
            var contractOfferId2 = UUID.randomUUID().toString();
            var endpoint = "http://endpoint";
            var assetId1 = UUID.randomUUID().toString();
            var assetId2 = UUID.randomUUID().toString();
            var entry1 = createCatalogBuilder(contractOfferId1, createAsset(assetId1), endpoint).build();
            var entry2 = createCatalogBuilder(contractOfferId2, createAsset(assetId2)).build();

            getStore().save(entry1);
            getStore().save(entry2);

            var query = QuerySpec.Builder.newInstance().filter(Criterion.criterion("dataServices.endpointUrl", "=", endpoint)).build();
            var result = getStore().query(query);

            assertThat(result)
                    .hasSize(1)
                    .allSatisfy(co -> assertThat(co.getDataServices().get(0).getEndpointUrl()).isEqualTo(endpoint));
        }
    }

    @Nested
    class Delete {

        @Test
        void removedMarked_noneMarked() {
            var contractOfferId1 = UUID.randomUUID().toString();
            var contractOfferId2 = UUID.randomUUID().toString();
            var assetId1 = UUID.randomUUID().toString();
            var assetId2 = UUID.randomUUID().toString();
            var entry1 = createCatalog(contractOfferId1, createAsset(assetId1));
            var entry2 = createCatalog(contractOfferId2, createAsset(assetId2));

            getStore().save(entry1);
            getStore().save(entry2);

            assertThat(getStore().query(QuerySpec.none())).hasSize(2);

            getStore().deleteExpired(); // none of them is marked, d
            assertThat(getStore().query(QuerySpec.none())).usingRecursiveFieldByFieldElementComparator().containsExactlyInAnyOrder(entry1, entry2);

        }

        @Test
        void removedMarked_shouldDeleteMarked() {
            var contractOfferId1 = UUID.randomUUID().toString();
            var contractOfferId2 = UUID.randomUUID().toString();
            var assetId1 = UUID.randomUUID().toString();
            var assetId2 = UUID.randomUUID().toString();
            var entry1 = createCatalog(contractOfferId1, createAsset(assetId1));
            var entry2 = createCatalog(contractOfferId2, createAsset(assetId2));

            getStore().save(entry1);
            getStore().save(entry2);

            assertThat(getStore().query(QuerySpec.none())).hasSize(2);

            getStore().expireAll(); // two items marked
            getStore().save(createCatalog(UUID.randomUUID().toString(), createAsset(UUID.randomUUID().toString())));
            getStore().deleteExpired(); // should delete only marked items
            assertThat(getStore().query(QuerySpec.none())).hasSize(1)
                    .doesNotContain(entry1, entry2);

        }
    }
}
