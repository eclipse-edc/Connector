/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.partner.cel;

import org.eclipse.edc.connector.controlplane.services.spi.partner.PartnerService;
import org.eclipse.edc.policy.cel.function.CelFunctionRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import static org.eclipse.edc.connector.controlplane.partner.cel.PartnerCelExtension.NAME;

/**
 * Registers the partner helper functions with the CEL engine, so policies can be written as
 * {@code ctx.partners.byIdentity(ctx.agent.id).inGroup('gold')}. When CEL is not part of the runtime the extension
 * does nothing.
 */
@Extension(NAME)
public class PartnerCelExtension implements ServiceExtension {

    public static final String NAME = "Partner Common Expression Language Extension";

    @Inject
    private PartnerService partnerService;

    @Inject(required = false)
    private CelFunctionRegistry celFunctionRegistry;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        if (celFunctionRegistry == null) {
            context.getMonitor().debug("CEL is not available, partner CEL functions will not be registered");
            return;
        }
        PartnerCelFunctions.functions(partnerService).forEach(celFunctionRegistry::registerFunction);
    }
}
