package org.eclipse.dataspaceconnector.spi.asset;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AssetSelectorExpressionTest {

    private AssetSelectorExpression expression;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void verifySerialization_emptyExpression() throws JsonProcessingException {
        expression = AssetSelectorExpression.Builder.newInstance().build();
        var json = objectMapper.writeValueAsString(expression);

        assertThat(json).isNotNull();
    }

    @Test
    void verify_selectAll() throws JsonProcessingException {
        expression = AssetSelectorExpression.SELECT_ALL;
        var json = objectMapper.writeValueAsString(expression);

        assertThat(json).isNotNull().contains("*");
        var expr = objectMapper.readValue(json, AssetSelectorExpression.class);
        assertThat(expr).isEqualTo(AssetSelectorExpression.SELECT_ALL);
    }

    @Test
    void verifySerialization() throws JsonProcessingException {
        expression = AssetSelectorExpression.Builder.newInstance()
                .constraint("name", "IN", "(bob, alice)")
                .build();
        var json = objectMapper.writeValueAsString(expression);
        assertThat(json).contains("name")
                .contains("IN")
                .contains("(bob, alice)");
    }

    @Test
    void verifyDeserialization() throws JsonProcessingException {
        expression = AssetSelectorExpression.Builder.newInstance()
                .constraint("name", "IN", "(bob, alice)")
                .build();
        var json = objectMapper.writeValueAsString(expression);

        var expr = objectMapper.readValue(json, AssetSelectorExpression.class);
        assertThat(expr.getCriteria()).hasSize(1)
                .allSatisfy(c -> {
                    assertThat(c.getOperandLeft()).isEqualTo("name");
                    assertThat(c.getOperator()).isEqualTo("IN");
                    assertThat(c.getOperandRight()).isEqualTo("(bob, alice)");
                });
    }
}