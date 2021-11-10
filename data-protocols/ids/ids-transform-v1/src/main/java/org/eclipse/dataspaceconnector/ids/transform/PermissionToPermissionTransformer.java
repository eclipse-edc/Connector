package org.eclipse.dataspaceconnector.ids.transform;

import de.fraunhofer.iais.eis.PermissionBuilder;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerContext;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

public class PermissionToPermissionTransformer implements IdsTypeTransformer<Permission, de.fraunhofer.iais.eis.Permission> {

    @Override
    public Class<Permission> getInputType() {
        return Permission.class;
    }

    @Override
    public Class<de.fraunhofer.iais.eis.Permission> getOutputType() {
        return de.fraunhofer.iais.eis.Permission.class;
    }

    @Override
    public @Nullable de.fraunhofer.iais.eis.Permission transform(Permission object, @NotNull TransformerContext context) {
        Objects.requireNonNull(context);
        if (object == null) {
            return null;
        }

        var idsConstraints = new ArrayList<de.fraunhofer.iais.eis.Constraint>();
        for (var edcConstraint : object.getConstraints()) {
            var idsConstraint = context.transform(edcConstraint, de.fraunhofer.iais.eis.Constraint.class);
            idsConstraints.add(idsConstraint);
        }

        var idsId = IdsId.Builder.newInstance().value(object.hashCode()).type(IdsType.PERMISSION).build();
        var id = context.transform(idsId, URI.class);
        var permissionBuilder = new PermissionBuilder(id);

        permissionBuilder._constraint_(idsConstraints);

        // TODO there is no transformer for strin to uri
        var target = object.getTarget();
        if (target != null) {
            permissionBuilder._target_(URI.create(target));
        }
        var assigner = object.getAssigner();
        if (assigner != null) {
            permissionBuilder._assigner_(new ArrayList<>(Collections.singletonList(URI.create(assigner))));
        }

        var assignee = object.getAssignee();
        if (assignee != null) {
            permissionBuilder._assignee_(new ArrayList<>(Collections.singletonList(URI.create(assignee))));
        }

        if (object.getAction() != null) {
            var action = context.transform(object.getAction(), de.fraunhofer.iais.eis.Action.class);
            permissionBuilder._action_(new ArrayList<>(Collections.singletonList(action)));
        }

        if (object.getDuty() != null) {
            // TODO Fix after the issue in the Information Model, asking about non-compliance between IDS and ODRL, is resolved
            // Link https://github.com/International-Data-Spaces-Association/InformationModel/issues/523
            context.reportProblem("Not supported transformation: EDC-Duty to IDS Pre-/Post-Duty");
        }

        return permissionBuilder.build();
    }
}
