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

package org.eclipse.edc.iam.decentralizedclaims.core;

import org.eclipse.edc.participantcontext.spi.config.ParticipantContextConfig;
import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.iam.decentralizedclaims.core.DcpCoreExtension.DEPRECATED_ISSUER_ID_KEY;
import static org.eclipse.edc.iam.decentralizedclaims.core.DcpCoreExtension.PARTICIPANT_DID;
import static org.eclipse.edc.iam.decentralizedclaims.core.DidConfigProvider.PARTICIPANT_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DidConfigProviderTest {

    private final ParticipantContextConfig config = mock();
    private final Monitor monitor = mock();
    private final DidConfigProvider provider = new DidConfigProvider(config, monitor);

    @Test
    void shouldReturnNullIfNoDidConfigured() {
        when(config.getString(any(), any(), any())).thenReturn(null);

        var did = provider.apply("participantContextId");

        assertThat(did).isNull();
        verify(monitor).severe(anyString());
    }

    @Test
    void shouldReturnConfiguredParticipantId() {
        when(config.getString(any(), eq(PARTICIPANT_ID), any())).thenReturn("did:participant");

        var did = provider.apply("participantContextId");

        assertThat(did).isEqualTo("did:participant");
    }

    @Test
    void shouldFallbackToParticipantDid_whenParticipantIdNotConfigured() {
        when(config.getString(any(), eq(PARTICIPANT_ID), any())).thenReturn(null);
        when(config.getString(any(), eq(PARTICIPANT_DID), any())).thenReturn("did:participant:value");

        var did = provider.apply("participantContextId");

        assertThat(did).isEqualTo("did:participant:value");
    }

    @Test
    void shouldFallbackToDeprecatedSetting_whenAllTheOthersAreNull() {
        when(config.getString(any(), eq(PARTICIPANT_ID), any())).thenReturn(null);
        when(config.getString(any(), eq(PARTICIPANT_DID), any())).thenReturn(null);
        when(config.getString(any(), eq(DEPRECATED_ISSUER_ID_KEY), any())).thenReturn("did:participant:deprecated");

        var did = provider.apply("participantContextId");

        assertThat(did).isEqualTo("did:participant:deprecated");
        verify(monitor).warning(anyString());
    }
}
