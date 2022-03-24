package org.eclipse.dataspaceconnector.api.datamanagement.asset;

import org.eclipse.dataspaceconnector.api.datamanagement.asset.service.AssetServiceImpl;
import org.eclipse.dataspaceconnector.dataloading.AssetLoader;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.transaction.NoopTransactionContext;
import org.eclipse.dataspaceconnector.spi.transaction.TransactionContext;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.api.result.ServiceFailure.Reason.CONFLICT;
import static org.eclipse.dataspaceconnector.api.result.ServiceFailure.Reason.NOT_FOUND;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AssetServiceImplTest {

    private final AssetIndex index = mock(AssetIndex.class);
    private final AssetLoader loader = mock(AssetLoader.class);
    private final ContractNegotiationStore contractNegotiationStore = mock(ContractNegotiationStore.class);
    private final TransactionContext dummyTransactionContext = new NoopTransactionContext();

    private final AssetServiceImpl service = new AssetServiceImpl(index, loader, contractNegotiationStore, dummyTransactionContext);

    @Test
    void findById_shouldRelyOnAssetIndex() {
        when(index.findById("assetId")).thenReturn(createAsset("assetId"));

        var asset = service.findbyId("assetId");

        String assetId = "assetId";
        assertThat(asset).isNotNull().matches(hasId(assetId));
    }

    @Test
    void query_shouldRelyOnAssetIndex() {
        var asset = createAsset("assetId");
        when(index.queryAssets(any(QuerySpec.class))).thenReturn(Stream.of(asset));

        var assets = service.query(QuerySpec.none());

        assertThat(assets).hasSize(1).first().matches(hasId("assetId"));
    }

    @Test
    void createAsset_shouldCreateAssetIfItDoesNotAlreadyExist() {
        var asset = createAsset("assetId");
        when(index.findById("assetId")).thenReturn(null);
        var dataAddress = DataAddress.Builder.newInstance().type("addressType").build();

        var inserted = service.create(asset, dataAddress);

        assertThat(inserted.succeeded()).isTrue();
        assertThat(inserted.getContent()).matches(hasId("assetId"));
        verify(loader).accept(argThat(it -> "assetId".equals(it.getId())), argThat(it -> "addressType".equals(it.getType())));
    }

    @Test
    void createAsset_shouldNotCreateAssetIfItAlreadyExists() {
        var asset = createAsset("assetId");
        when(index.findById("assetId")).thenReturn(asset);
        var dataAddress = DataAddress.Builder.newInstance().type("addressType").build();

        var inserted = service.create(asset, dataAddress);

        assertThat(inserted.succeeded()).isFalse();
        verifyNoInteractions(loader);
    }

    @Test
    void delete_shouldDeleteAssetIfItsNotReferencedByAnyNegotiation() {
        when(contractNegotiationStore.queryNegotiations(any())).thenReturn(Stream.empty());
        when(loader.deleteById("assetId")).thenReturn(createAsset("assetId"));

        var deleted = service.delete("assetId");

        assertThat(deleted.succeeded()).isTrue();
        assertThat(deleted.getContent()).matches(hasId("assetId"));
    }

    @Test
    void delete_shouldNotDeleteIfAssetIsAlreadyPartOfAnAgreement() {
        var asset = createAsset("assetId");
        when(loader.deleteById("assetId")).thenReturn(asset);
        ContractNegotiation contractNegotiation = ContractNegotiation.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .counterPartyId(UUID.randomUUID().toString())
                .counterPartyAddress("address")
                .protocol("protocol")
                .contractAgreement(ContractAgreement.Builder.newInstance()
                        .id(UUID.randomUUID().toString())
                        .providerAgentId(UUID.randomUUID().toString())
                        .consumerAgentId(UUID.randomUUID().toString())
                        .asset(asset)
                        .policy(Policy.Builder.newInstance().build())
                        .build())
                .build();
        when(contractNegotiationStore.queryNegotiations(any())).thenReturn(Stream.of(contractNegotiation));

        var deleted = service.delete("assetId");

        assertThat(deleted.failed()).isTrue();
        assertThat(deleted.getFailure().getReason()).isEqualTo(CONFLICT);
    }

    @Test
    void delete_shouldFailIfAssetDoesNotExist() {
        when(loader.deleteById("assetId")).thenReturn(null);

        var deleted = service.delete("assetId");

        assertThat(deleted.failed()).isTrue();
        assertThat(deleted.getFailure().getReason()).isEqualTo(NOT_FOUND);
    }

    @NotNull
    private Predicate<Asset> hasId(String assetId) {
        return it -> assetId.equals(it.getId());
    }

    private Asset createAsset(String assetId) {
        return Asset.Builder.newInstance().id(assetId).build();
    }
}