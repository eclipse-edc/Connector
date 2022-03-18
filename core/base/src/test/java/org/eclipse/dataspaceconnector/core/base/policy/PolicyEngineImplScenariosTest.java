package org.eclipse.dataspaceconnector.core.base.policy;

import org.eclipse.dataspaceconnector.policy.model.Action;
import org.eclipse.dataspaceconnector.policy.model.AtomicConstraint;
import org.eclipse.dataspaceconnector.policy.model.LiteralExpression;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.Prohibition;
import org.eclipse.dataspaceconnector.spi.agent.ParticipantAgent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.policy.model.Operator.IN;

/**
 * Tests key policy enforcement scenarios. Also serves as a blueprint for custom policy functions.
 */
public class PolicyEngineImplScenariosTest {
    private static final String ABS_SPATIAL_CONSTRAINT = "absoluteSpatialPosition";
    private static final String CONNECTOR_CONSTRAINT = "connector";
    private static final Action USE_ACTION = Action.Builder.newInstance().type("USE").build();

    private PolicyEngineImpl policyEngine;

    /**
     * Demonstrates how to evaluate a simple policy.
     */
    @Test
    void verifyUnrestrictedUse() {
        var usePermission = Permission.Builder.newInstance().action(USE_ACTION).build();

        var policy = Policy.Builder.newInstance().permission(usePermission).build();

        var agent = new ParticipantAgent(emptyMap(), emptyMap());
        assertThat(policyEngine.evaluate(policy, agent).succeeded()).isTrue();
    }

    /**
     * Demonstrates how to use a rule function.
     */
    @Test
    void verifyNoUse() {
        var prohibition = Prohibition.Builder.newInstance().action(USE_ACTION).build();

        var policy = Policy.Builder.newInstance().prohibition(prohibition).build();

        var agent = new ParticipantAgent(emptyMap(), emptyMap());

        policyEngine.registerFunction(Prohibition.class, (rule, context) -> rule.getAction().getType().equals(USE_ACTION.getType()));
        assertThat(policyEngine.evaluate(policy, agent).succeeded()).isFalse();
    }

    /**
     * Demonstrates a spatial constraint and how to evaluate policy against agent claims.
     */
    @Test
    void verifySpatialLocation() {
        // function that verifies the EU region
        policyEngine.registerFunction(Permission.class, ABS_SPATIAL_CONSTRAINT, (operator, value, permission, context) -> {
            var claims = context.getParticipantAgent().getClaims();
            return claims.containsKey("region") && claims.get("region").equals(value);
        });

        var left = new LiteralExpression(ABS_SPATIAL_CONSTRAINT);
        var right = new LiteralExpression("eu");
        var spatialConstraint = AtomicConstraint.Builder.newInstance().leftExpression(left).operator(IN).rightExpression(right).build();

        var usePermission = Permission.Builder.newInstance().action(USE_ACTION).constraint(spatialConstraint).build();

        var policy = Policy.Builder.newInstance().permission(usePermission).build();

        var euAgent = new ParticipantAgent(Map.of("region", "eu"), emptyMap());
        assertThat(policyEngine.evaluate(policy, euAgent).succeeded()).isTrue();

        var noRegionAgent = new ParticipantAgent(emptyMap(), emptyMap());
        assertThat(policyEngine.evaluate(policy, noRegionAgent).succeeded()).isFalse();
    }

    /**
     * Shows how to handle literal types that are derived from JSON objects.
     */
    @Test
    void verifyConnectorUse() {
        policyEngine.registerFunction(Permission.class, CONNECTOR_CONSTRAINT, (operator, value, permission, context) -> {
            if (!(value instanceof List)) {
                context.reportProblem("Unsupported right operand type: " + value.getClass().getName());
                return false;
            }
            //noinspection rawtypes
            return ((List) value).contains("connector1");
        });

        var left = new LiteralExpression(CONNECTOR_CONSTRAINT);
        var right = new LiteralExpression(List.of("connector1"));
        var connectorConstraint = AtomicConstraint.Builder.newInstance().leftExpression(left).operator(IN).rightExpression(right).build();
        var usePermission = Permission.Builder.newInstance().action(USE_ACTION).constraint(connectorConstraint).build();

        var policy = Policy.Builder.newInstance().permission(usePermission).build();

        var agent = new ParticipantAgent(emptyMap(), emptyMap());
        assertThat(policyEngine.evaluate(policy, agent).succeeded()).isTrue();
    }

    @BeforeEach
    void setUp() {
        policyEngine = new PolicyEngineImpl();
    }

}
