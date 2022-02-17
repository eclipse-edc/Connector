package org.eclipse.dataspaceconnector.ids.policy;

import org.eclipse.dataspaceconnector.policy.model.Operator;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.spi.contract.agent.ParticipantAgent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.policy.model.Operator.EQ;
import static org.eclipse.dataspaceconnector.policy.model.Operator.NEQ;

class PartnerLevelConstraintFunctionTest {

    private final PartnerLevelConstraintFunction constraintFunction = new PartnerLevelConstraintFunction();

    @Test
    void shouldVerifyEqConstraint() {
        var agent = new ParticipantAgent(Map.of("partnerLevel", "gold"), emptyMap());

        boolean result = constraintFunction.evaluate(EQ, "gold", Permission.Builder.newInstance().build(), new MockPolicyContext(agent));

        assertThat(result).isTrue();
    }

    @Test
    void shouldNotVerifyEqConstraint() {
        var agent = new ParticipantAgent(Map.of("partnerLevel", "gold"), emptyMap());

        boolean result = constraintFunction.evaluate(EQ, "silver", Permission.Builder.newInstance().build(), new MockPolicyContext(agent));

        assertThat(result).isFalse();
    }

    @Test
    void shouldVerifyNotEqConstraint() {
        var agent = new ParticipantAgent(Map.of("partnerLevel", "gold"), emptyMap());

        boolean result = constraintFunction.evaluate(NEQ, "silver", Permission.Builder.newInstance().build(), new MockPolicyContext(agent));

        assertThat(result).isTrue();
    }

    @Test
    void shouldNotVerifyNotEqConstraint() {
        var agent = new ParticipantAgent(Map.of("partnerLevel", "gold"), emptyMap());

        boolean result = constraintFunction.evaluate(NEQ, "gold", Permission.Builder.newInstance().build(), new MockPolicyContext(agent));

        assertThat(result).isFalse();
    }


    @Test
    void shouldVerifyInConstraint() {
        var agent = new ParticipantAgent(Map.of("partnerLevel", "gold"), emptyMap());

        boolean result = constraintFunction.evaluate(Operator.IN, List.of("gold"), Permission.Builder.newInstance().build(), new MockPolicyContext(agent));

        assertThat(result).isTrue();
    }

    @Test
    void shouldNotVerifyInConstraint() {
        var agent = new ParticipantAgent(Map.of("partnerLevel", "silver"), emptyMap());

        boolean result = constraintFunction.evaluate(Operator.IN, List.of("gold"), Permission.Builder.newInstance().build(), new MockPolicyContext(agent));

        assertThat(result).isFalse();
    }
}
