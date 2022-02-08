package org.eclipse.dataspaceconnector.catalog.store;

import org.eclipse.dataspaceconnector.catalog.spi.FederatedCacheStore;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.query.CriterionConverter;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryFederatedCacheStoreTest {

    private FederatedCacheStore store;

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
        store = new InMemoryFederatedCacheStore(converter);
    }

    @Test
    void queryCacheContainingOneElementWithNoCriterion_shouldReturnUniqueElement() {
        String contractOfferId = UUID.randomUUID().toString();
        String assetId = UUID.randomUUID().toString();
        ContractOffer contractOffer = createContractOffer(contractOfferId, createAsset(assetId));

        store.save(contractOffer);

        Collection<ContractOffer> result = store.query(Collections.emptyList());

        assertThat(result)
                .hasSize(1)
                .allSatisfy(co -> assertThat(co.getAsset().getId()).isEqualTo(assetId));
    }

    @Test
    void queryCacheAfterInsertingSameAssetTwice_shouldReturnLastInsertedContractOfferOnly() {
        String contractOfferId1 = UUID.randomUUID().toString();
        String contractOfferId2 = UUID.randomUUID().toString();
        String assetId = UUID.randomUUID().toString();
        ContractOffer contractOffer1 = createContractOffer(contractOfferId1, createAsset(assetId));
        ContractOffer contractOffer2 = createContractOffer(contractOfferId2, createAsset(assetId));

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
        String contractOfferId1 = UUID.randomUUID().toString();
        String contractOfferId2 = UUID.randomUUID().toString();
        String assetId1 = UUID.randomUUID().toString();
        String assetId2 = UUID.randomUUID().toString();
        ContractOffer contractOffer1 = createContractOffer(contractOfferId1, createAsset(assetId1));
        ContractOffer contractOffer2 = createContractOffer(contractOfferId2, createAsset(assetId2));

        store.save(contractOffer1);
        store.save(contractOffer2);

        Collection<ContractOffer> result = store.query(Collections.emptyList());

        assertThat(result)
                .hasSize(2)
                .anySatisfy(co -> assertThat(co.getAsset().getId()).isEqualTo(assetId1))
                .anySatisfy(co -> assertThat(co.getAsset().getId()).isEqualTo(assetId2));
    }
}