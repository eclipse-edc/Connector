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

package org.eclipse.edc.policy.context.request;

import org.eclipse.edc.controlplane.ProtocolRemoteMessage;
import org.eclipse.edc.policy.context.request.spi.RequestScopeExtractor;
import org.eclipse.edc.policy.context.request.spi.RequestScopeExtractorRegistry;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.protocol.spi.RequestContext;
import org.eclipse.edc.spi.result.Result;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RequestScopeExtractorRegistryImpl implements RequestScopeExtractorRegistry {

    private final List<ExtractorEntry> extractors = new ArrayList<>();

    @Override
    public void registerExtractor(Class<? extends ProtocolRemoteMessage> messageType, RequestScopeExtractor extractor) {
        extractors.add(new ExtractorEntry(messageType, extractor));
    }

    @Override
    public Result<Set<String>> extractScopes(Policy policy, RequestContext context) {
        var scopes = new HashSet<String>();
        for (var entry : extractors) {
            if (entry.messageType().isAssignableFrom(context.getMessage().getClass())) {
                var result = entry.extractor().extractScopes(policy, context);
                if (result.failed()) {
                    return Result.failure(result.getFailureMessages());
                }
                scopes.addAll(result.getContent());
            }
        }
        return Result.success(scopes);
    }

    private record ExtractorEntry(Class<? extends ProtocolRemoteMessage> messageType, RequestScopeExtractor extractor) {
    }
}
