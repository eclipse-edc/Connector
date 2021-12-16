package org.eclipse.dataspaceconnector.catalog.cache;

import org.eclipse.dataspaceconnector.catalog.spi.WorkItem;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.jetbrains.annotations.NotNull;

public class TestUtil {

    public static WorkItem createWorkItem() {
        return new WorkItem("test-url", "test-protocol");
    }

    @NotNull
    public static ContractOffer createOffer(String id) {
        return ContractOffer.Builder.newInstance()
                .id(id)
                .asset(Asset.Builder.newInstance().id(id).build())
                .policy(Policy.Builder.newInstance().build())
                .build();
    }

}
