package org.eclipse.dataspaceconnector.clients.postgresql.asset.operation;

import org.assertj.core.api.Assertions;
import org.eclipse.dataspaceconnector.clients.postgresql.asset.Repository;
import org.eclipse.dataspaceconnector.common.annotations.IntegrationTest;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.asset.Criterion;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@IntegrationTest
public class ContractDefinitionQueryOperationTest extends AbstractOperationTest {

    private Repository repository;

    @BeforeEach
    public void setup() {
        repository = getRepository();
    }

    @Test
    public void testContractDefinitionQueryAll() throws SQLException {
        Criterion criterion = new Criterion("hello", "=", "world");
        AssetSelectorExpression selectorExpression = AssetSelectorExpression.Builder.newInstance()
                .criteria(Collections.singletonList(criterion))
                .build();
        Policy contractPolicy = Policy.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .build();
        Policy accessPolicy = Policy.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .build();

        ContractDefinition contractDefinition = ContractDefinition.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .selectorExpression(selectorExpression)
                .contractPolicy(contractPolicy)
                .accessPolicy(accessPolicy)
                .build();

        ContractDefinition contractDefinition2 = ContractDefinition.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .selectorExpression(selectorExpression)
                .contractPolicy(contractPolicy)
                .accessPolicy(accessPolicy)
                .build();

        ContractDefinition contractDefinition3 = ContractDefinition.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .selectorExpression(selectorExpression)
                .contractPolicy(contractPolicy)
                .accessPolicy(accessPolicy)
                .build();

        repository.create(contractDefinition);
        repository.create(contractDefinition2);
        repository.create(contractDefinition3);

        List<ContractDefinition> storedDefinitions = repository.queryAllContractDefinitions();

        Assertions.assertThat(storedDefinitions).size().isEqualTo(3);
    }
}
