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

package org.eclipse.edc.iam.decentralizedclaims.sts.remote.registrar;

import org.eclipse.edc.iam.decentralizedclaims.spi.SecureTokenServiceRegistry;
import org.eclipse.edc.iam.decentralizedclaims.sts.remote.RemoteSecureTokenService;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfiguration;
import org.eclipse.edc.participantcontext.spi.config.validation.ParticipantConfigEntry;
import org.eclipse.edc.participantcontext.spi.config.validation.ParticipantConfigView;
import org.eclipse.edc.participantcontext.spi.config.validation.ParticipantContextConfigValidatorRegistry;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.decentralizedclaims.spi.SecureTokenServiceRegistry.STS_TYPE_CONFIG_KEY;
import static org.eclipse.edc.iam.decentralizedclaims.sts.remote.registrar.StsRemoteRegistrarExtension.OAUTH_STS_TYPE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class StsRemoteRegistrarExtensionTest {

    private final SecureTokenServiceRegistry registry = mock();
    private final ParticipantContextConfigValidatorRegistry configValidatorRegistry = mock();

    @BeforeEach
    void setup(ServiceExtensionContext context) {
        context.registerService(SecureTokenServiceRegistry.class, registry);
        context.registerService(ParticipantContextConfigValidatorRegistry.class, configValidatorRegistry);
    }

    @Test
    void initialize_shouldRegisterRemoteStsUnderOauthType(StsRemoteRegistrarExtension extension, ServiceExtensionContext context) {
        extension.initialize(context);

        verify(registry).register(eq(OAUTH_STS_TYPE), any(RemoteSecureTokenService.class));
    }

    @Test
    void initialize_shouldRegisterConfigEntries_requiredWhenOauthTypeIsSelected(StsRemoteRegistrarExtension extension, ServiceExtensionContext context) {
        when(registry.defaultType()).thenReturn(OAUTH_STS_TYPE);
        var captor = ArgumentCaptor.forClass(ParticipantConfigEntry.class);

        extension.initialize(context);

        verify(configValidatorRegistry, times(3)).register(captor.capture());
        assertThat(captor.getAllValues()).extracting(ParticipantConfigEntry::getKey)
                .containsExactlyInAnyOrder("edc.iam.sts.oauth.token.url", "edc.iam.sts.oauth.client.id", "edc.iam.sts.oauth.client.secret.alias");
        assertThat(captor.getAllValues()).allSatisfy(entry -> {
            assertThat(entry.isRequired(view(Map.of()))).isTrue();
            assertThat(entry.isRequired(view(Map.of(STS_TYPE_CONFIG_KEY, OAUTH_STS_TYPE)))).isTrue();
            assertThat(entry.isRequired(view(Map.of(STS_TYPE_CONFIG_KEY, "signature")))).isFalse();
        });
    }

    private ParticipantConfigView view(Map<String, String> entries) {
        return ParticipantConfigView.of(ParticipantContextConfiguration.Builder.newInstance().participantContextId("id").entries(entries).build());
    }

}
