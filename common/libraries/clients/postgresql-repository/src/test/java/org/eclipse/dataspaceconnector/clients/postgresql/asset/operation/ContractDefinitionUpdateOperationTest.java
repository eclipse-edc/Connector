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
public class ContractDefinitionUpdateOperationTest extends AbstractOperationTest {

    private Repository repository;

    @BeforeEach
    public void setup() {
        repository = getRepository();
    }

    @Test
    public void testContractDefinitionCreation() throws SQLException {

        ContractDefinition baseDefinition = createDefinition();

        repository.create(baseDefinition);

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
                .id(baseDefinition.getId())
                .selectorExpression(selectorExpression)
                .contractPolicy(contractPolicy)
                .accessPolicy(accessPolicy)
                .build();

        repository.update(contractDefinition);

        List<ContractDefinition> storedDefinitions = repository.queryAllContractDefinitions();
        ContractDefinition storedDefinition = storedDefinitions.stream()
                .filter(d -> d.getId().equals(contractDefinition.getId()))
                .findFirst()
                .orElse(null);

        Assertions.assertThat(storedDefinition).isNotNull();
        Assertions.assertThat(storedDefinition.getSelectorExpression()).isNotNull();
        Assertions.assertThat(storedDefinition.getSelectorExpression().getCriteria().get(0)).isEqualTo(criterion);
        Assertions.assertThat(storedDefinition.getAccessPolicy()).isNotNull();
        Assertions.assertThat(storedDefinition.getAccessPolicy().getUid()).isEqualTo(accessPolicy.getUid());
        Assertions.assertThat(storedDefinition.getContractPolicy()).isNotNull();
        Assertions.assertThat(storedDefinition.getContractPolicy().getUid()).isEqualTo(contractPolicy.getUid());
    }

    private ContractDefinition createDefinition() {
        AssetSelectorExpression selectorExpression = AssetSelectorExpression.Builder.newInstance()
                .build();
        Policy contractPolicy = Policy.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .build();
        Policy accessPolicy = Policy.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .build();

        return ContractDefinition.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .selectorExpression(selectorExpression)
                .contractPolicy(contractPolicy)
                .accessPolicy(accessPolicy)
                .build();
    }
}
