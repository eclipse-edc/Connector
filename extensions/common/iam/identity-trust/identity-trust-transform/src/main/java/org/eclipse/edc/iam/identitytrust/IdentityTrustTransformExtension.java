/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.iam.identitytrust;

import org.eclipse.edc.iam.identitytrust.transform.to.JsonObjectToCredentialStatusTransformer;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

@Extension(value = IdentityTrustTransformExtension.NAME, categories = { "iam", "transform", "jsonld" })
public class IdentityTrustTransformExtension implements ServiceExtension {
    public static final String NAME = "Identity And Trust Transform Extension";

    @Inject
    private TypeTransformerRegistry typeTransformerRegistry;

    @Override
    public void initialize(ServiceExtensionContext context) {
        ServiceExtension.super.initialize(context);

        typeTransformerRegistry.register(new JsonObjectToCredentialStatusTransformer());
    }
}
