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

package org.eclipse.edc.participantcontext.config.validation;

import org.eclipse.edc.participantcontext.spi.config.model.ParticipantContextConfiguration;
import org.eclipse.edc.participantcontext.spi.config.validation.ParticipantConfigEntry;
import org.eclipse.edc.participantcontext.spi.config.validation.ParticipantConfigView;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Violation;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.participantcontext.spi.config.validation.ParticipantConfigValidators.longValue;

class ParticipantContextConfigValidatorRegistryImplTest {

    private final ParticipantContextConfigValidatorRegistryImpl registry = new ParticipantContextConfigValidatorRegistryImpl(false);

    private static ParticipantContextConfiguration config(Map<String, String> entries, Map<String, String> privateEntries) {
        return ParticipantContextConfiguration.Builder.newInstance()
                .participantContextId("participantContextId")
                .entries(entries)
                .privateEntries(privateEntries)
                .build();
    }

    private static ParticipantConfigView view(ParticipantContextConfiguration configuration) {
        return ParticipantConfigView.of(configuration);
    }

    @Nested
    class ValidateWrite {

        @Test
        void shouldSucceed_whenNoEntryRegistered() {
            var write = config(Map.of("any", "value"), Map.of("secret", "value"));

            assertThat(registry.validateWrite(write, view(write))).isSucceeded();
        }

        @Test
        void shouldFail_whenValueInvalid() {
            registry.register(ParticipantConfigEntry.Builder.newInstance("number").validator(longValue(1, 10)).build());
            var write = config(Map.of("number", "11"), Map.of());

            var result = registry.validateWrite(write, view(write));

            assertThat(result).isFailed();
            assertThat(result.getFailure().getViolations()).containsExactly(
                    new Violation("'number' must be between 1 and 10", "number", "11"));
        }

        @Test
        void shouldNotEchoPrivateValues() {
            registry.register(ParticipantConfigEntry.Builder.newInstance("secret").sensitive().validator(longValue()).build());
            var write = config(Map.of(), Map.of("secret", "s3cr3t"));

            var result = registry.validateWrite(write, view(write));

            assertThat(result).isFailed();
            assertThat(result.getFailure().getViolations()).singleElement().satisfies(violation -> {
                assertThat(violation.value()).isNull();
                assertThat(violation.message()).doesNotContain("s3cr3t");
            });
        }

        @Test
        void shouldFail_whenSensitiveKeyIsPublic() {
            registry.register(ParticipantConfigEntry.Builder.newInstance("secret").sensitive().build());
            var write = config(Map.of("secret", "value"), Map.of());

            assertThat(registry.validateWrite(write, view(write))).isFailed().messages()
                    .containsExactly("'secret' is sensitive and must be set as a private entry");
        }

        @Test
        void shouldFail_whenPublicKeyIsPrivate() {
            registry.register(ParticipantConfigEntry.Builder.newInstance("public").build());
            var write = config(Map.of(), Map.of("public", "value"));

            assertThat(registry.validateWrite(write, view(write))).isFailed().messages()
                    .containsExactly("'public' is not sensitive and must be set as a public entry");
        }

        @Test
        void shouldNotValidateRemovals() {
            registry.register(ParticipantConfigEntry.Builder.newInstance("key").validator(value -> {
                throw new AssertionError("removals must not be validated");
            }).build());
            var entries = new HashMap<String, String>();
            entries.put("key", null);
            var patch = config(entries, Map.of());

            assertThat(registry.validateWrite(patch, view(config(Map.of(), Map.of())))).isSucceeded();
        }

        @Test
        void shouldCheckRequiredKeys_onEffectiveConfiguration() {
            registry.register(ParticipantConfigEntry.Builder.newInstance("required").required().build());
            var patch = config(Map.of("other", "value"), Map.of());

            assertThat(registry.validateWrite(patch, view(config(Map.of("other", "value"), Map.of())))).isFailed().messages()
                    .containsExactly("'required' is required");
            assertThat(registry.validateWrite(patch, view(config(Map.of("other", "value"), Map.of("required", "encrypted"))))).isSucceeded();
        }

        @Test
        void shouldCheckConditionallyRequiredKeys() {
            registry.register(ParticipantConfigEntry.Builder.newInstance("url")
                    .requiredWhen(view -> view.getEntry("type").filter("remote"::equals).isPresent())
                    .build());

            var local = config(Map.of("type", "local"), Map.of());
            assertThat(registry.validateWrite(local, view(local))).isSucceeded();

            var remote = config(Map.of("type", "remote"), Map.of());
            assertThat(registry.validateWrite(remote, view(remote))).isFailed().messages().containsExactly("'url' is required");
        }

        @Test
        void shouldCollectAllViolations() {
            registry.register(ParticipantConfigEntry.Builder.newInstance("number").validator(longValue()).build());
            registry.register(ParticipantConfigEntry.Builder.newInstance("secret").sensitive().build());
            registry.register(ParticipantConfigEntry.Builder.newInstance("required").required().build());
            var write = config(Map.of("number", "nan", "secret", "value"), Map.of());

            var result = registry.validateWrite(write, view(write));

            assertThat(result).isFailed();
            assertThat(result.getFailure().getViolations()).extracting(Violation::path)
                    .containsExactlyInAnyOrder("number", "secret", "required");
        }

        @Test
        void shouldFail_whenStrictAndKeyUnknown() {
            var strictRegistry = new ParticipantContextConfigValidatorRegistryImpl(true);
            strictRegistry.register(ParticipantConfigEntry.Builder.newInstance("known").build());
            var write = config(Map.of("known", "value", "unknown", "value"), Map.of("unknownSecret", "value"));

            assertThat(strictRegistry.validateWrite(write, view(write))).isFailed().messages().containsExactlyInAnyOrder(
                    "'unknown' is not a known configuration key", "'unknownSecret' is not a known configuration key");
        }
    }

    @Nested
    class Validate {

        @Test
        void shouldSucceed_whenValid() {
            registry.register(ParticipantConfigEntry.Builder.newInstance("number").validator(longValue()).required().build());
            registry.register(ParticipantConfigEntry.Builder.newInstance("secret").sensitive().required().build());

            assertThat(registry.validate(view(config(Map.of("number", "1"), Map.of("secret", "encrypted"))))).isSucceeded();
        }

        @Test
        void shouldReportInvalidPublicValues_missingKeysAndMisplacedKeys() {
            registry.register(ParticipantConfigEntry.Builder.newInstance("number").validator(longValue()).build());
            registry.register(ParticipantConfigEntry.Builder.newInstance("public").build());
            registry.register(ParticipantConfigEntry.Builder.newInstance("required").required().build());

            var result = registry.validate(view(config(Map.of("number", "nan"), Map.of("public", "encrypted"))));

            assertThat(result).isFailed();
            assertThat(result.getFailure().getViolations()).extracting(Violation::path)
                    .containsExactlyInAnyOrder("number", "public", "required");
        }

        @Test
        void shouldNotValidatePrivateValues() {
            registry.register(ParticipantConfigEntry.Builder.newInstance("secret").sensitive()
                    .validator(value -> ValidationResult.failure(Violation.violation("invalid", null)))
                    .build());

            assertThat(registry.validate(view(config(Map.of(), Map.of("secret", "encrypted"))))).isSucceeded();
        }
    }
}
