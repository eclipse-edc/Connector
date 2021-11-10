package org.eclipse.dataspaceconnector.ids.transform;

import de.fraunhofer.iais.eis.BinaryOperator;
import de.fraunhofer.iais.eis.ConstraintBuilder;
import de.fraunhofer.iais.eis.LeftOperand;
import de.fraunhofer.iais.eis.util.RdfResource;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerContext;
import org.eclipse.dataspaceconnector.policy.model.AtomicConstraint;
import org.eclipse.dataspaceconnector.policy.model.Constraint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.Objects;

public class ConstraintToConstraintTransformer implements IdsTypeTransformer<Constraint, de.fraunhofer.iais.eis.Constraint> {

    @Override
    public Class<Constraint> getInputType() {
        return Constraint.class;
    }

    @Override
    public Class<de.fraunhofer.iais.eis.Constraint> getOutputType() {
        return de.fraunhofer.iais.eis.Constraint.class;
    }

    @Override
    public @Nullable de.fraunhofer.iais.eis.Constraint transform(Constraint object, @NotNull TransformerContext context) {
        Objects.requireNonNull(context);
        if (object == null) {
            return null;
        }

        if (!(object instanceof AtomicConstraint)) {
            context.reportProblem(String.format("Cannot transform %s. Supported Constraints: '%s'", object.getClass().getName(), AtomicConstraint.class.getName()));
            return null;
        }

        var atomicConstraint = (AtomicConstraint) object;

        LeftOperand leftOperand = context.transform(atomicConstraint.getLeftExpression(), LeftOperand.class);
        RdfResource rightOperand = context.transform(atomicConstraint.getRightExpression(), RdfResource.class);
        BinaryOperator operator = context.transform(atomicConstraint.getOperator(), BinaryOperator.class);

        var idsId = IdsId.Builder.newInstance().value(atomicConstraint.hashCode()).type(IdsType.CONSTRAINT).build();
        var id = context.transform(idsId, URI.class);
        var constraintBuilder = new ConstraintBuilder(id);

        constraintBuilder._leftOperand_(leftOperand);
        constraintBuilder._rightOperand_(rightOperand);
        constraintBuilder._operator_(operator);

        return constraintBuilder.build();
    }
}
