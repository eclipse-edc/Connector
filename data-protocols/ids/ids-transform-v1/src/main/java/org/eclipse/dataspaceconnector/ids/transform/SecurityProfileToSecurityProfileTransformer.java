package org.eclipse.dataspaceconnector.ids.transform;

import de.fraunhofer.iais.eis.SecurityProfile;
import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerContext;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SecurityProfileToSecurityProfileTransformer implements IdsTypeTransformer<org.eclipse.dataspaceconnector.ids.spi.types.SecurityProfile, SecurityProfile> {
    private static final Map<org.eclipse.dataspaceconnector.ids.spi.types.SecurityProfile, SecurityProfile> MAPPING = new HashMap<>() {
        {
            put(org.eclipse.dataspaceconnector.ids.spi.types.SecurityProfile.BASE_SECURITY_PROFILE, SecurityProfile.BASE_SECURITY_PROFILE);
            put(org.eclipse.dataspaceconnector.ids.spi.types.SecurityProfile.TRUST_SECURITY_PROFILE, SecurityProfile.TRUST_SECURITY_PROFILE);
            put(org.eclipse.dataspaceconnector.ids.spi.types.SecurityProfile.TRUST_PLUS_SECURITY_PROFILE, SecurityProfile.TRUST_PLUS_SECURITY_PROFILE);
        }
    };

    @Override
    public Class<org.eclipse.dataspaceconnector.ids.spi.types.SecurityProfile> getInputType() {
        return org.eclipse.dataspaceconnector.ids.spi.types.SecurityProfile.class;
    }

    @Override
    public Class<SecurityProfile> getOutputType() {
        return SecurityProfile.class;
    }

    @Override
    public @Nullable SecurityProfile transform(org.eclipse.dataspaceconnector.ids.spi.types.SecurityProfile object, TransformerContext context) {
        Objects.requireNonNull(context);
        if (object == null) {
            return null;
        }

        return MAPPING.get(object);
    }
}
