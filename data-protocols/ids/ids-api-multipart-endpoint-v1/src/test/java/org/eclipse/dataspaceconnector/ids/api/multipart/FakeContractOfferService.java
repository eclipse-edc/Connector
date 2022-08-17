package org.eclipse.dataspaceconnector.ids.api.multipart;

import org.eclipse.dataspaceconnector.policy.model.Action;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.PolicyType;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractOfferQuery;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractOfferService;
import org.eclipse.dataspaceconnector.spi.message.Range;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Stream;

class FakeContractOfferService implements ContractOfferService {
    private final List<Asset> assets;

    FakeContractOfferService(List<Asset> assets) {
        this.assets = assets;
    }

    @Override
    @NotNull
    public Stream<ContractOffer> queryContractOffers(ContractOfferQuery query, Range range) {
        return assets.stream().map(asset ->
                ContractOffer.Builder.newInstance()
                        .id("1")
                        .policy(createEverythingAllowedPolicy())
                        .asset(asset)
                        .build()
        );
    }

    private Policy createEverythingAllowedPolicy() {
        var policyBuilder = Policy.Builder.newInstance();
        var permissionBuilder = Permission.Builder.newInstance();
        var actionBuilder = Action.Builder.newInstance();

        policyBuilder.type(PolicyType.CONTRACT);
        actionBuilder.type("USE");
        permissionBuilder.target("1");

        permissionBuilder.action(actionBuilder.build());
        policyBuilder.permission(permissionBuilder.build());

        policyBuilder.target("1");
        return policyBuilder.build();
    }
}
