/*
 *  Copyright (c) 2024 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.policy.context.request.spi;

import org.eclipse.edc.policy.engine.spi.PolicyContextImpl;
import org.eclipse.edc.spi.iam.RequestContext;
import org.eclipse.edc.spi.iam.RequestScope;

/**
 * Policy Context for "request.*" scope
 */
public abstract class RequestPolicyContext extends PolicyContextImpl {

    private final RequestContext requestContext;
    private final RequestScope.Builder requestScopeBuilder;

    protected RequestPolicyContext(RequestContext requestContext, RequestScope.Builder requestScopeBuilder) {
        this.requestContext = requestContext;
        this.requestScopeBuilder = requestScopeBuilder;
    }

    public RequestContext requestContext() {
        return requestContext;
    }

    public RequestScope.Builder requestScopeBuilder() {
        return requestScopeBuilder;
    }

    @FunctionalInterface
    public interface Provider {
        RequestPolicyContext instantiate(RequestContext requestContext, RequestScope.Builder requestScopeBuilder);
    }
}
