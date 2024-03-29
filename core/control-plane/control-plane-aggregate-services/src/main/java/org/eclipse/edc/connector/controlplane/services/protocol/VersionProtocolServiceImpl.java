/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.connector.controlplane.services.protocol;

import org.eclipse.edc.connector.controlplane.services.spi.protocol.ProtocolTokenValidator;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.ProtocolVersionRegistry;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.ProtocolVersions;
import org.eclipse.edc.connector.controlplane.services.spi.protocol.VersionProtocolService;
import org.eclipse.edc.policy.engine.spi.PolicyScope;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.ServiceResult;

public class VersionProtocolServiceImpl implements VersionProtocolService {

    @PolicyScope
    public static final String VERSIONING_REQUEST_SCOPE = "request.version";

    private final ProtocolVersionRegistry registry;
    private final ProtocolTokenValidator tokenValidator;

    public VersionProtocolServiceImpl(ProtocolVersionRegistry registry, ProtocolTokenValidator tokenValidator) {
        this.registry = registry;
        this.tokenValidator = tokenValidator;
    }

    @Override
    public ServiceResult<ProtocolVersions> getAll(TokenRepresentation tokenRepresentation) {
        return tokenValidator.verify(tokenRepresentation, VERSIONING_REQUEST_SCOPE)
                .map(it -> registry.getAll());
    }
}
