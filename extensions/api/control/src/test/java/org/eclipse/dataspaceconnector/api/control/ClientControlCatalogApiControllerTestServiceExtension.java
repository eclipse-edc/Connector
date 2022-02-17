package org.eclipse.dataspaceconnector.api.control;

import org.eclipse.dataspaceconnector.dataloading.AssetLoader;
import org.eclipse.dataspaceconnector.policy.model.Action;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.response.NegotiationResult;
import org.eclipse.dataspaceconnector.spi.contract.negotiation.store.ContractNegotiationStore;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractOfferRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.command.ContractNegotiationCommand;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

class ClientControlCatalogApiControllerTestServiceExtension implements ServiceExtension {

    @Inject
    private AssetLoader assetLoader;
    @Inject
    private ContractDefinitionStore contractDefinitionStore;

    @Override
    public String name() {
        return "EDC Control API Test";
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        assetLoader = context.getService(AssetLoader.class);
        contractDefinitionStore = context.getService(ContractDefinitionStore.class);
        context.registerService(ConsumerContractNegotiationManager.class, new FakeConsumerNegotiationManager());
        context.registerService(ContractNegotiationStore.class, new FakeContractNegotiationStore());
    }

    @Override
    public void start() {
        Asset asset = Asset.Builder.newInstance()
                .properties(new HashMap<String, Object>() {
                    {
                        put("ids:fileName", "filename1");
                        put("ids:byteSize", 1234);
                        put("ids:fileExtension", "txt");
                    }
                })
                .id("1").build();
        DataAddress dataAddress = DataAddress.Builder.newInstance()
                .type("AzureStorage")
                .property("account", "edc101")
                .property("container", "provider")
                .property("blobname", "data1.txt")
                .keyName("provider-blob-storage-key")
                .build();
        assetLoader.accept(asset, dataAddress);

        Asset asset2 = Asset.Builder.newInstance()
                .properties(new HashMap<String, Object>() {
                    {
                        put("ids:fileName", "filename2");
                        put("ids:byteSize", 5678);
                        put("ids:fileExtension", "pdf");
                    }
                })
                .id("2").build();
        DataAddress dataAddress2 = DataAddress.Builder.newInstance()
                .type("AzureStorage")
                .property("account", "edc101")
                .property("container", "provider")
                .property("blobname", "data2.txt")
                .keyName("provider-blob-storage-key")
                .build();
        assetLoader.accept(asset2, dataAddress2);

        Policy publicPolicy = Policy.Builder.newInstance()
                .permission(Permission.Builder.newInstance()
                        .target("1")
                        .action(Action.Builder.newInstance()
                                .type("USE")
                                .build())
                        .build())
                .build();

        Policy publicPolicy2 = Policy.Builder.newInstance()
                .permission(Permission.Builder.newInstance()
                        .target("2")
                        .action(Action.Builder.newInstance()
                                .type("USE")
                                .build())
                        .build())
                .build();

        ContractDefinition contractDefinition1 = ContractDefinition.Builder.newInstance()
                .id("1")
                .accessPolicy(publicPolicy)
                .contractPolicy(publicPolicy)
                .selectorExpression(AssetSelectorExpression.Builder.newInstance().whenEquals("asset:prop:id", "1").build())
                .build();

        ContractDefinition contractDefinition2 = ContractDefinition.Builder.newInstance()
                .id("2")
                .accessPolicy(publicPolicy2)
                .contractPolicy(publicPolicy2)
                .selectorExpression(AssetSelectorExpression.Builder.newInstance().whenEquals("asset:prop:id", "2").build())
                .build();

        contractDefinitionStore.save(contractDefinition1);
        contractDefinitionStore.save(contractDefinition2);
    }

    private static class FakeContractNegotiationStore implements ContractNegotiationStore {

        @Override
        public @Nullable ContractNegotiation find(String negotiationId) {
            return null;
        }

        @Override
        public @Nullable ContractNegotiation findForCorrelationId(String correlationId) {
            return null;
        }

        @Override
        public @Nullable ContractAgreement findContractAgreement(String contractId) {
            return null;
        }

        @Override
        public void save(ContractNegotiation negotiation) {

        }

        @Override
        public void delete(String negotiationId) {

        }

        @Override
        public @NotNull List<ContractNegotiation> nextForState(int state, int max) {
            return null;
        }

        @Override
        public Stream<ContractNegotiation> queryNegotiations(QuerySpec querySpec) {
            throw new UnsupportedOperationException();
        }
    }

    private static class FakeConsumerNegotiationManager implements ConsumerContractNegotiationManager {

        @Override
        public NegotiationResult initiate(ContractOfferRequest contractOffer) {
            return null;
        }

        @Override
        public NegotiationResult offerReceived(ClaimToken token, String negotiationId, ContractOffer contractOffer, String hash) {
            return null;
        }

        @Override
        public NegotiationResult confirmed(ClaimToken token, String negotiationId,
                                           ContractAgreement contract, String hash) {
            return null;
        }

        @Override
        public NegotiationResult declined(ClaimToken token, String negotiationId) {
            return null;
        }

        @Override
        public void enqueueCommand(ContractNegotiationCommand command) {
        }
    }
}
