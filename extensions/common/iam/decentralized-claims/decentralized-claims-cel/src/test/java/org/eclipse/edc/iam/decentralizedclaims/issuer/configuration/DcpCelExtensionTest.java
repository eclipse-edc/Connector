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

package org.eclipse.edc.iam.decentralizedclaims.issuer.configuration;

import org.eclipse.edc.iam.decentralizedclaims.cel.DcpCelExtension;
import org.eclipse.edc.iam.decentralizedclaims.cel.VcClaimMapper;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.policy.cel.function.CelFunction;
import org.eclipse.edc.policy.cel.function.CelFunctionRegistry;
import org.eclipse.edc.policy.cel.function.CelFunctionRegistryImpl;
import org.eclipse.edc.policy.cel.function.context.CelParticipantAgentClaimMapperRegistry;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(DependencyInjectionExtension.class)
public class DcpCelExtensionTest {

    private final CelParticipantAgentClaimMapperRegistry claimMapperRegistry = mock();
    private final CelFunctionRegistry celFunctionRegistry = new CelFunctionRegistryImpl();

    @BeforeEach
    void setup(ServiceExtensionContext context) {
        context.registerService(CelParticipantAgentClaimMapperRegistry.class, claimMapperRegistry);
        context.registerService(CelFunctionRegistry.class, celFunctionRegistry);
        context.registerService(Clock.class, Clock.systemUTC());
    }

    @Test
    void initialize(ServiceExtensionContext context, DcpCelExtension ext) {
        ext.initialize(context);

        verify(claimMapperRegistry).registerClaimMapper(isA(VcClaimMapper.class));
    }

    @Test
    void initialize_registersVcFunctions(ServiceExtensionContext context, DcpCelExtension ext) {
        ext.initialize(context);

        assertThat(celFunctionRegistry.functions())
                .isNotEmpty()
                .extracting(CelFunction::name)
                .contains("withType", "withContext", "withIssuer", "valid", "hasCredential", "hasClaim", "claim", "claims", "hasType", "hasContext");
        // overload ids must be unique, otherwise the CEL runtime rejects the bindings
        assertThat(celFunctionRegistry.functions()).extracting(CelFunction::overloadId).doesNotHaveDuplicates();
    }

}
