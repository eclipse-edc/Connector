package org.eclipse.dataspaceconnector.ids.transform;

import de.fraunhofer.iais.eis.Action;
import de.fraunhofer.iais.eis.Constraint;
import de.fraunhofer.iais.eis.ProhibitionBuilder;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerContext;
import org.eclipse.dataspaceconnector.policy.model.Prohibition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

public class ProhibitionToProhibitionTransformer implements IdsTypeTransformer<Prohibition, de.fraunhofer.iais.eis.Prohibition> {

    @Override
    public Class<Prohibition> getInputType() {
        return Prohibition.class;
    }

    @Override
    public Class<de.fraunhofer.iais.eis.Prohibition> getOutputType() {
        return de.fraunhofer.iais.eis.Prohibition.class;
    }

    @Override
    public @Nullable de.fraunhofer.iais.eis.Prohibition transform(Prohibition object, @NotNull TransformerContext context) {
        Objects.requireNonNull(context);
        if (object == null) {
            return null;
        }

        var constraints = new ArrayList<Constraint>();
        for (var edcConstraint : object.getConstraints()) {
            var idsConstraint = context.transform(edcConstraint, Constraint.class);
            constraints.add(idsConstraint);
        }

        var idsId = IdsId.Builder.newInstance().value(object.hashCode()).type(IdsType.PROHIBITION).build();
        var id = context.transform(idsId, URI.class);
        var prohibitionBuilder = new ProhibitionBuilder(id);

        var action = context.transform(object.getAction(), Action.class);
        var assinger = object.getAssigner();
        if (assinger != null) {
            prohibitionBuilder._assigner_(new ArrayList<>(Collections.singletonList(URI.create(assinger))));
        }

        var assignee = object.getAssignee();
        if (assignee != null) {
            prohibitionBuilder._assignee_(new ArrayList<>(Collections.singletonList(URI.create(assignee))));
        }

        var target = object.getTarget();
        if (target != null) {
            prohibitionBuilder._target_(URI.create(target));
        }

        prohibitionBuilder._action_(new ArrayList<>(Collections.singletonList(action)));
        prohibitionBuilder._constraint_(constraints);

        return prohibitionBuilder.build();
    }
}
