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

package org.eclipse.edc.iam.decentralizedclaims.sts.registry;

import org.eclipse.edc.iam.decentralizedclaims.spi.SecureTokenService;
import org.eclipse.edc.iam.decentralizedclaims.spi.SecureTokenServiceRegistry;
import org.eclipse.edc.participantcontext.spi.config.ParticipantContextConfig;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * A {@link SecureTokenService} that dispatches {@code createToken} requests to a concrete implementation resolved from
 * the {@link SecureTokenServiceRegistry}. The implementation to use is determined per participant context by reading
 * the {@code edc.iam.sts} property from the participant context configuration. If the property is not set, the
 * configured default type is used.
 */
public class DelegatingSecureTokenService implements SecureTokenService {

    public static final String STS_TYPE_PROPERTY = "edc.iam.sts.type";

    private final SecureTokenServiceRegistry registry;
    private final ParticipantContextConfig participantContextConfig;
    private final String defaultType;

    public DelegatingSecureTokenService(SecureTokenServiceRegistry registry, ParticipantContextConfig participantContextConfig, String defaultType) {
        this.registry = registry;
        this.participantContextConfig = participantContextConfig;
        this.defaultType = defaultType;
    }

    @Override
    public Result<TokenRepresentation> createToken(String participantContextId, Map<String, Object> claims, @Nullable String bearerAccessScope) {
        var type = participantContextConfig.getString(participantContextId, STS_TYPE_PROPERTY, defaultType);
        var secureTokenService = registry.resolve(type);
        if (secureTokenService == null) {
            return Result.failure("No SecureTokenService registered for type '%s' (participant context '%s')".formatted(type, participantContextId));
        }
        return secureTokenService.createToken(participantContextId, claims, bearerAccessScope);
    }
}
