/*
 *  Copyright (c) 2026 Think-it GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Think-it GmbH - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.http.dispatcher;

import org.eclipse.edc.connector.controlplane.catalog.spi.CatalogRequestMessage;
import org.eclipse.edc.policy.context.request.spi.RequestScopeExtractor;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.protocol.spi.RequestContext;
import org.eclipse.edc.spi.result.Result;

import java.util.HashSet;
import java.util.Set;

/**
 * Extracts the additional, user-supplied scopes carried by a {@link CatalogRequestMessage}. Registered against
 * {@link CatalogRequestMessage}, so the message is always of that type when this runs.
 */
public class CatalogAdditionalScopesExtractor implements RequestScopeExtractor {

    @Override
    public Result<Set<String>> extractScopes(Policy policy, RequestContext context) {
        if (context.getMessage() instanceof CatalogRequestMessage catalogRequestMessage) {
            return Result.success(new HashSet<>(catalogRequestMessage.getAdditionalScopes()));
        }
        return Result.success(Set.of());
    }
}
