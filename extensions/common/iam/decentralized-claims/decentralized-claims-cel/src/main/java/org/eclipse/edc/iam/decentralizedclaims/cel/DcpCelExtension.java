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

package org.eclipse.edc.iam.decentralizedclaims.cel;

import org.eclipse.edc.policy.cel.function.context.CelParticipantAgentClaimMapperRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import static org.eclipse.edc.iam.decentralizedclaims.cel.DcpCelExtension.NAME;

/**
 * Common Expression Language extensions for decentralized claims processing.
 */
@Extension(NAME)
public class DcpCelExtension implements ServiceExtension {
    
    protected static final String NAME = "DCP Common Expression Language Extensions";

    @Inject
    private CelParticipantAgentClaimMapperRegistry claimMapperRegistry;

    @Override
    public void initialize(ServiceExtensionContext context) {
        claimMapperRegistry.registerClaimMapper(new VcClaimMapper());
    }


}
