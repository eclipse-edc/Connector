package org.eclipse.dataspaceconnector.spi.types.domain.contract.offer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContractDefinitionTest {

    @Test
    void verifySerializeDeserialize() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        var definition = ContractDefinition.Builder.newInstance()
                .id("1")
                .accessPolicy(Policy.Builder.newInstance().build()).contractPolicy(Policy.Builder.newInstance().build()).selectorExpression(AssetSelectorExpression.SELECT_ALL).build();
        var serialized = mapper.writeValueAsString(definition);

        var deserialized = mapper.readValue(serialized, ContractDefinition.class);

        assertThat(deserialized).isNotNull();
    }
}
