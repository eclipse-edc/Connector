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
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.policy.cel.function.CelFunction;
import org.eclipse.edc.policy.cel.function.CelFunctionRegistry;
import org.eclipse.edc.policy.cel.function.CelFunctionRegistryImpl;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(DependencyInjectionExtension.class)
class PartnerCelExtensionTest {

    private final CelFunctionRegistry celFunctionRegistry = new CelFunctionRegistryImpl();

    @BeforeEach
    void setup(ServiceExtensionContext context) {
        context.registerService(PartnerService.class, mock());
        context.registerService(CelFunctionRegistry.class, celFunctionRegistry);
    }

    @Test
    void initialize_registersPartnerFunctions(ServiceExtensionContext context, PartnerCelExtension extension) {
        extension.initialize(context);

        assertThat(celFunctionRegistry.functions())
                .extracting(CelFunction::name)
                .containsExactlyInAnyOrder("byIdentity", "byId", "query", "inGroup", "groups", "found");
        assertThat(celFunctionRegistry.functions()).extracting(CelFunction::overloadId).doesNotHaveDuplicates();
    }
}
