/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial Implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.transform;

import org.eclipse.dataspaceconnector.ids.spi.transform.IdsTypeTransformer;
import org.eclipse.dataspaceconnector.ids.spi.types.SecurityProfile;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SecurityProfileToIdsSecurityProfileTransformer implements IdsTypeTransformer<SecurityProfile, de.fraunhofer.iais.eis.SecurityProfile> {
    private static final Map<SecurityProfile, de.fraunhofer.iais.eis.SecurityProfile> MAPPING = new HashMap<>() {
        {
            put(SecurityProfile.BASE_SECURITY_PROFILE, de.fraunhofer.iais.eis.SecurityProfile.BASE_SECURITY_PROFILE);
            put(SecurityProfile.TRUST_SECURITY_PROFILE, de.fraunhofer.iais.eis.SecurityProfile.TRUST_SECURITY_PROFILE);
            put(SecurityProfile.TRUST_PLUS_SECURITY_PROFILE, de.fraunhofer.iais.eis.SecurityProfile.TRUST_PLUS_SECURITY_PROFILE);
        }
    };

    @Override
    public Class<SecurityProfile> getInputType() {
        return SecurityProfile.class;
    }

    @Override
    public Class<de.fraunhofer.iais.eis.SecurityProfile> getOutputType() {
        return de.fraunhofer.iais.eis.SecurityProfile.class;
    }

    @Override
    public @Nullable de.fraunhofer.iais.eis.SecurityProfile transform(SecurityProfile object, @NotNull TransformerContext context) {
        Objects.requireNonNull(context);
        if (object == null) {
            return null;
        }

        return MAPPING.get(object);
    }
}
