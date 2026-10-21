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

package org.eclipse.edc.api.authorization.filter;

import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.FeatureContext;
import org.eclipse.edc.api.auth.spi.RequiredScope;
import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScopeBasedAccessFeatureTest {

    private final Monitor monitor = mock();
    private final ResourceInfo resourceInfo = mock();
    private final FeatureContext featureContext = mock();
    private final ScopeBasedAccessFeature feature = new ScopeBasedAccessFeature(monitor);

    @Test
    void shouldRegisterScopeFilter_whenMethodDeclaresRequiredScope() throws NoSuchMethodException {
        when(resourceInfo.getResourceMethod()).thenReturn(TestResource.class.getMethod("withScope"));
        doReturnResourceClass();

        feature.configure(resourceInfo, featureContext);

        verify(featureContext).register(isA(ScopeBasedAccessFilter.class));
        verify(featureContext, never()).register(isA(DenyAccessFilter.class));
        verify(monitor, never()).warning(anyString());
    }

    @Test
    void shouldDenyAccess_whenMethodDoesNotDeclareRequiredScope() throws NoSuchMethodException {
        when(resourceInfo.getResourceMethod()).thenReturn(TestResource.class.getMethod("withoutScope"));
        doReturnResourceClass();

        feature.configure(resourceInfo, featureContext);

        verify(featureContext).register(isA(DenyAccessFilter.class));
        verify(featureContext, never()).register(isA(ScopeBasedAccessFilter.class));
        verify(monitor).warning(anyString());
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void shouldNotRegisterAnyFilter_whenResourceIsGeneratedByJersey() throws ClassNotFoundException {
        // e.g. the OPTIONS handlers that Jersey generates for every resource
        var jerseyClass = Class.forName("org.glassfish.jersey.internal.RuntimeDelegateImpl");
        when(resourceInfo.getResourceClass()).thenReturn((Class) jerseyClass);
        when(resourceInfo.getResourceMethod()).thenReturn(jerseyClass.getMethods()[0]);

        feature.configure(resourceInfo, featureContext);

        verify(featureContext, never()).register(any(Object.class));
        verify(monitor, never()).warning(anyString());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void doReturnResourceClass() {
        when(resourceInfo.getResourceClass()).thenReturn((Class) TestResource.class);
    }

    public static class TestResource {

        @RequiredScope("management-api:read")
        public void withScope() {
        }

        public void withoutScope() {
        }
    }
}
