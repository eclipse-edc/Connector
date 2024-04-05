package org.eclipse.edc.connector.controlplane.services.policydefinition;

import org.eclipse.edc.policy.model.AndConstraint;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.Constraint;
import org.eclipse.edc.policy.model.Expression;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.policy.model.MultiplicityConstraint;
import org.eclipse.edc.policy.model.OrConstraint;
import org.eclipse.edc.policy.model.XoneConstraint;

import java.util.List;
import java.util.Map;

public class PolicyDefinitionServiceSubtypesProvider {
    public Map<Class<?>, List<Class<?>>> getSubtypeMap() {
        return Map.of(
                Constraint.class, List.of(MultiplicityConstraint.class, AtomicConstraint.class),
                MultiplicityConstraint.class, List.of(AndConstraint.class, OrConstraint.class, XoneConstraint.class),
                Expression.class, List.of(LiteralExpression.class)
        );
    }
}