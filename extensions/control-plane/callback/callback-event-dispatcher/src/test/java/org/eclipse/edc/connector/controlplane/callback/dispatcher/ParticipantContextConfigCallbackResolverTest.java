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

package org.eclipse.edc.connector.controlplane.callback.dispatcher;

import org.eclipse.edc.json.JacksonTypeManager;
import org.eclipse.edc.participantcontext.spi.config.ParticipantContextConfig;
import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.controlplane.callback.dispatcher.ParticipantContextConfigCallbackResolver.CALLBACKS_CONFIG_KEY;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ParticipantContextConfigCallbackResolverTest {

    private final ParticipantContextConfig config = mock();
    private final Monitor monitor = mock();
    private final ParticipantContextConfigCallbackResolver resolver =
            new ParticipantContextConfigCallbackResolver(config, new JacksonTypeManager()::getMapper, monitor);

    @Test
    void shouldReturnCallbacksMatchingTheEvent() {
        when(config.getString("participantContextId", CALLBACKS_CONFIG_KEY, "")).thenReturn("""
                [
                  {"uri": "http://held", "events": ["contract.negotiation.held"], "transactional": false, "authKey": "key", "authCodeId": "code"},
                  {"uri": "http://all", "events": ["contract.negotiation"]},
                  {"uri": "http://transfer", "events": ["transfer.process"]}
                ]
                """);

        var callbacks = resolver.resolve("participantContextId", "contract.negotiation.held");

        assertThat(callbacks).hasSize(2).satisfiesExactly(
                held -> {
                    assertThat(held.getUri()).isEqualTo("http://held");
                    assertThat(held.isTransactional()).isFalse();
                    assertThat(held.getAuthKey()).isEqualTo("key");
                    assertThat(held.getAuthCodeId()).isEqualTo("code");
                },
                all -> assertThat(all.getUri()).isEqualTo("http://all"));
    }

    @Test
    void shouldReturnEmpty_whenNoEntry() {
        when(config.getString("participantContextId", CALLBACKS_CONFIG_KEY, "")).thenReturn("");

        assertThat(resolver.resolve("participantContextId", "contract.negotiation.held")).isEmpty();
    }

    @Test
    void shouldReturnEmptyAndWarn_whenEntryIsInvalid() {
        when(config.getString("participantContextId", CALLBACKS_CONFIG_KEY, "")).thenReturn("not-a-json");

        assertThat(resolver.resolve("participantContextId", "contract.negotiation.held")).isEmpty();
        verify(monitor).warning(anyString());
    }
}
