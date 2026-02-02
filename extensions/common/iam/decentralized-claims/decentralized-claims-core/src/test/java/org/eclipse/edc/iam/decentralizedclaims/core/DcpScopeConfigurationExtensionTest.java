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

package org.eclipse.edc.iam.decentralizedclaims.core;

import org.eclipse.edc.iam.decentralizedclaims.spi.scope.DcpScope;
import org.eclipse.edc.iam.decentralizedclaims.spi.scope.DcpScopeRegistry;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
public class DcpScopeConfigurationExtensionTest {

    private final DcpScopeRegistry registry = mock();

    @BeforeEach
    void setup(ServiceExtensionContext context) {
        context.registerService(DcpScopeRegistry.class, registry);
        when(registry.register(any())).thenReturn(ServiceResult.success());
    }

    @Test
    void initialize(ServiceExtensionContext context, DynamicDcpScopeConfigurationExtension ext) {
        var cfg = ConfigFactory.fromMap(Map.of(
                "membership.id", "membership-scope",
                "membership.value", "org.eclipse.edc.vc.type:MembershipCredential:read",
                "membership.type", "DEFAULT"));
        when(context.getConfig("edc.iam.dcp.scopes")).thenReturn(cfg);

        ext.initialize(context);

        var captor = ArgumentCaptor.forClass(DcpScope.class);
        verify(registry).register(captor.capture());

        assertThat(captor.getValue().getId()).satisfies(scope -> {
            assertThat(scope).isEqualTo("membership-scope");
            assertThat(captor.getValue().getValue()).isEqualTo("org.eclipse.edc.vc.type:MembershipCredential:read");
            assertThat(captor.getValue().getType()).isEqualTo(DcpScope.Type.DEFAULT);
        });
    }

    @Test
    void initialize_withPolicyType(ServiceExtensionContext context, DynamicDcpScopeConfigurationExtension ext) {
        var cfg = ConfigFactory.fromMap(Map.of(
                "membership.id", "membership-scope",
                "membership.prefix-mapping", "Membership.",
                "membership.value", "org.eclipse.edc.vc.type:MembershipCredential:read",
                "membership.type", "POLICY"));
        when(context.getConfig("edc.iam.dcp.scopes")).thenReturn(cfg);

        ext.initialize(context);

        var captor = ArgumentCaptor.forClass(DcpScope.class);
        verify(registry).register(captor.capture());

        assertThat(captor.getValue()).satisfies(scope -> {
            assertThat(scope.getId()).isEqualTo("membership-scope");
            assertThat(scope.getValue()).isEqualTo("org.eclipse.edc.vc.type:MembershipCredential:read");
            assertThat(scope.getType()).isEqualTo(DcpScope.Type.POLICY);
            assertThat(scope.getPrefixMapping()).isEqualTo("Membership.");
        });
    }

}
