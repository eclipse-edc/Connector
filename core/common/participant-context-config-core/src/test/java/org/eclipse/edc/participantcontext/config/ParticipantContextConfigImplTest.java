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

package org.eclipse.edc.participantcontext.config;

import org.eclipse.edc.encryption.EncryptionAlgorithmRegistry;
import org.eclipse.edc.participantcontext.spi.config.ParticipantContextConfig;
import org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfiguration;
import org.eclipse.edc.participantcontext.spi.config.store.ParticipantContextConfigStore;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.Map;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class ParticipantContextConfigImplTest {

    private static final String PARTICIPANT_CONTEXT_ID = "participantContextId";
    private final ParticipantContextConfigStore store = mock();
    private final EncryptionAlgorithmRegistry registry = mock();
    private final ParticipantContextConfig contextConfig = new ParticipantContextConfigImpl(registry, "any", store, new NoopTransactionContext());

    @ParameterizedTest
    @ArgumentsSource(SettingProvider.class)
    void shouldGetSetting(SettingCall setting, String key, String value, Object expectedValue) {

        var cfg = ParticipantContextConfiguration.Builder.newInstance().participantContextId(PARTICIPANT_CONTEXT_ID)
                .entries(Map.of(key, value))
                .build();

        when(store.get(PARTICIPANT_CONTEXT_ID)).thenReturn(cfg);

        var result = setting.call(contextConfig, PARTICIPANT_CONTEXT_ID, key);

        assertThat(result).isNotNull()
                .isEqualTo(expectedValue);

    }

    @ParameterizedTest
    @ArgumentsSource(SettingProviderWithDefault.class)
    <T> void shouldGetSettingWithDefault(SettingCallWithDefault<T> setting, String key, T defaultValue) {

        var cfg = ParticipantContextConfiguration.Builder.newInstance().participantContextId(PARTICIPANT_CONTEXT_ID).build();
        when(store.get(PARTICIPANT_CONTEXT_ID)).thenReturn(cfg);

        var result = setting.call(contextConfig, PARTICIPANT_CONTEXT_ID, key, defaultValue);

        assertThat(result).isNotNull()
                .isEqualTo(defaultValue);
    }


    @ParameterizedTest
    @ArgumentsSource(SettingProvider.class)
    void notFound(SettingCall setting, String key, String value, Object expectedValue) {

        when(store.get(PARTICIPANT_CONTEXT_ID)).thenReturn(null);

        assertThatThrownBy(() -> setting.call(contextConfig, PARTICIPANT_CONTEXT_ID, key)).isInstanceOf(EdcException.class)
                .hasMessageContaining("No configuration found for participant context");

    }

    @FunctionalInterface
    private interface SettingCall {
        Object call(ParticipantContextConfig service, String participantContextId, String key);
    }

    @FunctionalInterface
    private interface SettingCallWithDefault<T> {
        Object call(ParticipantContextConfig service, String participantContextId, String key, T defaultValue);
    }

    private static class SettingProvider implements ArgumentsProvider {

        SettingCall getBoolean = ParticipantContextConfig::getBoolean;
        SettingCall getString = ParticipantContextConfig::getString;
        SettingCall getInteger = ParticipantContextConfig::getInteger;
        SettingCall getLong = ParticipantContextConfig::getLong;

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    Arguments.of(getBoolean, "config.boolean", "true", true),
                    Arguments.of(getString, "config.string", "test", "test"),
                    Arguments.of(getInteger, "config.integer", "10", 10),
                    Arguments.of(getLong, "config.long", "10", 10L)
            );
        }
    }

    private static class SettingProviderWithDefault implements ArgumentsProvider {

        SettingCallWithDefault<Boolean> getBoolean = ParticipantContextConfig::getBoolean;
        SettingCallWithDefault<String> getString = ParticipantContextConfig::getString;
        SettingCallWithDefault<Integer> getInteger = ParticipantContextConfig::getInteger;
        SettingCallWithDefault<Long> getLong = ParticipantContextConfig::getLong;

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            return Stream.of(
                    Arguments.of(getBoolean, "config.boolean", true),
                    Arguments.of(getString, "config.string", "test"),
                    Arguments.of(getInteger, "config.integer", 10),
                    Arguments.of(getLong, "config.long", 10L)
            );
        }
    }

    @Nested
    class GetSensitiveString {

        @Test
        void shouldGetPrivateSetting() {
            var cfg = ParticipantContextConfiguration.Builder.newInstance().participantContextId(PARTICIPANT_CONTEXT_ID)
                    .entries(Map.of("key", "value"))
                    .privateEntries(Map.of("private.key", "encryptedValue"))
                    .build();

            when(registry.decrypt("any", "encryptedValue")).thenReturn(Result.success("decryptedValue"));
            when(store.get(PARTICIPANT_CONTEXT_ID)).thenReturn(cfg);

            var result = contextConfig.getSensitiveString(PARTICIPANT_CONTEXT_ID, "private.key");

            assertThat(result).isNotNull()
                    .isEqualTo("decryptedValue");
        }

        @Test
        void shouldReturnNull_whenNoSettingFound() {
            var cfg = ParticipantContextConfiguration.Builder.newInstance().participantContextId(PARTICIPANT_CONTEXT_ID)
                    .entries(emptyMap())
                    .privateEntries(emptyMap())
                    .build();
            when(store.get(PARTICIPANT_CONTEXT_ID)).thenReturn(cfg);

            var result = contextConfig.getSensitiveString(PARTICIPANT_CONTEXT_ID, "any");

            assertThat(result).isNull();
            verifyNoInteractions(registry);
        }
    }
}
