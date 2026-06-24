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

import org.eclipse.edc.policy.engine.spi.PolicyScope;
import org.eclipse.edc.spi.iam.RequestContext;
import org.eclipse.edc.spi.iam.RequestScope;

public class RequestCatalogPolicyContext extends RequestPolicyContext {
    /**
     * Policy scope evaluated when an outgoing catalog request is made
     */
    @PolicyScope
    public static final String CATALOGING_REQUEST_SCOPE = "request.catalog";

    public RequestCatalogPolicyContext(RequestContext requestContext, RequestScope.Builder requestScopeBuilder) {
        super(requestContext, requestScopeBuilder);
    }

    @Override
    public String scope() {
        return CATALOGING_REQUEST_SCOPE;
    }
}
