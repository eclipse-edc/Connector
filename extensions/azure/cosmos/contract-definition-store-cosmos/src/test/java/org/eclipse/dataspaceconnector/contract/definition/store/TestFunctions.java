package org.eclipse.dataspaceconnector.contract.definition.store;

import org.eclipse.dataspaceconnector.contract.definition.store.model.ContractDefinitionDocument;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;

import java.util.UUID;

public class TestFunctions {

    public static final String PARTITION_KEY = "test-ap-id1";

    public static ContractDefinition generateDefinition() {
        return ContractDefinition.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .contractPolicy(Policy.Builder.newInstance().id("test-cp-id1").build())
                .accessPolicy(Policy.Builder.newInstance().id(PARTITION_KEY).build())
                .selectorExpression(AssetSelectorExpression.Builder.newInstance().whenEquals("somekey", "someval").build())
                .build();
    }

    public static ContractDefinitionDocument generateDocument() {
        return new ContractDefinitionDocument(generateDefinition());
    }
}
