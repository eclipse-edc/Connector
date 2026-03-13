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
import org.eclipse.edc.policy.cel.function.context.CelParticipantAgentClaimMapperRegistry;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(DependencyInjectionExtension.class)
public class DcpCelExtensionTest {

    private final CelParticipantAgentClaimMapperRegistry claimMapperRegistry = mock();

    @BeforeEach
    void setup(ServiceExtensionContext context) {
        context.registerService(CelParticipantAgentClaimMapperRegistry.class, claimMapperRegistry);
    }

    @Test
    void initialize(ServiceExtensionContext context, DcpCelExtension ext) {
        ext.initialize(context);

        verify(claimMapperRegistry).registerClaimMapper(isA(VcClaimMapper.class));
    }

}
