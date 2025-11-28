/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
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

package org.eclipse.edc.identityhub.api.authorization.filter;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.container.DynamicFeature;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.FeatureContext;

/**
 * This feature will inspect the {@link ResourceInfo#getResourceMethod()} of a resolved resource (=endpoint) and check if it is
 * annotated with the {@link RolesAllowed} annotation, and if so, a {@link RoleBasedAccessFilter} is registered for the specific
 * request context of that endpoint.
 */
public class RoleBasedAccessFeature implements DynamicFeature {

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext featureContext) {
        var annotation = resourceInfo.getResourceMethod().getAnnotation(RolesAllowed.class);
        if (annotation != null) {
            featureContext.register(new RoleBasedAccessFilter(annotation.value()));
        }
    }
}
