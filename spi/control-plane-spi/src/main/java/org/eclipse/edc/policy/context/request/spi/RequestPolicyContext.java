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
import org.eclipse.edc.protocol.spi.RequestContext;

/**
 * Policy Context for "request.*" scope
 */
public abstract class RequestPolicyContext extends PolicyContextImpl {

    private final RequestContext requestContext;

    protected RequestPolicyContext(RequestContext requestContext) {
        this.requestContext = requestContext;
    }

    public RequestContext requestContext() {
        return requestContext;
    }

    @FunctionalInterface
    public interface Provider {
        RequestPolicyContext instantiate(RequestContext requestContext);
    }
}
