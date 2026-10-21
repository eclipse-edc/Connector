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

package org.eclipse.edc.policy.context.request.spi;

import org.eclipse.edc.controlplane.ProtocolRemoteMessage;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.protocol.spi.RequestContext;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.result.Result;

import java.util.Set;

/**
 * Registry for {@link RequestScopeExtractor}s. It owns the message-type dispatch (which extractors apply to a given
 * request) and the combination of their results.
 */
@ExtensionPoint
public interface RequestScopeExtractorRegistry {

    /**
     * Register an extractor for a given {@link ProtocolRemoteMessage} type. An extractor is invoked for a request when
     * the registered {@code messageType} is assignable from the request message's runtime type.
     *
     * @param messageType the message type the extractor applies to.
     * @param extractor   the extractor.
     */
    void registerExtractor(Class<? extends ProtocolRemoteMessage> messageType, RequestScopeExtractor extractor);

    /**
     * Extract the request scopes using all extractors registered for the request message's type, returning the union of
     * their results. Fails as soon as one extractor returns a failure.
     *
     * @param policy  the policy associated with the request.
     * @param context the request context.
     * @return the union of the extracted scopes if all succeeded, otherwise the first failure.
     */
    Result<Set<String>> extractScopes(Policy policy, RequestContext context);
}
