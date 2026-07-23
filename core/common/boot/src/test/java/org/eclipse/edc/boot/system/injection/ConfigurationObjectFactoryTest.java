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

package org.eclipse.edc.boot.system.injection;

import org.eclipse.edc.junit.extensions.TestExtensionContext;
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.runtime.metamodel.annotation.Settings;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfigurationObjectFactoryTest {

    private final TestExtensionContext context = new TestExtensionContext();
    private final ConfigurationObjectFactory factory = new ConfigurationObjectFactory();

    @Nested
    class FromClass {
        @Test
        void shouldInstantiateConfigurationObjectClass() {
            context.setConfig(ConfigFactory.fromMap(Map.of(
                    "required", "requiredValue",
                    "optional", "optionalValue"
            )));

            var object = factory.instantiate(context, null, ConfigurationClass.class);

            assertThat(object).isInstanceOfSatisfying(ConfigurationClass.class, configuration -> {
                assertThat(configuration.getRequired()).isEqualTo("requiredValue");
                assertThat(configuration.getOptional()).isEqualTo("optionalValue");
            });
        }

        @Test
        void shouldInstantiateConfigurationObjectClassWithPrefix() {
            context.setConfig(ConfigFactory.fromMap(Map.of(
                    "prefix.required", "requiredValue",
                    "prefix.optional", "optionalValue"
            )));

            var object = factory.instantiate(context, "prefix", ConfigurationClass.class);

            assertThat(object).isInstanceOfSatisfying(ConfigurationClass.class, configuration -> {
                assertThat(configuration.getRequired()).isEqualTo("requiredValue");
                assertThat(configuration.getOptional()).isEqualTo("optionalValue");
            });
        }

        @Test
        void shouldThrowException_whenRequiredValueIsMissing() {
            context.setConfig(ConfigFactory.fromMap(Map.of(
                    "optional", "optionalValue"
            )));

            assertThatThrownBy(() -> factory.instantiate(context, null, ConfigurationClass.class))
                    .isInstanceOf(EdcInjectionException.class);
        }

        @Settings
        public static class ConfigurationClass {

            @Setting(key = "optional", required = false)
            private String optional;

            @Setting(key = "required", required = true)
            private String required;

            public String getOptional() {
                return optional;
            }

            public String getRequired() {
                return required;
            }
        }
    }

    @Nested
    class FromRecord {
        @Test
        void shouldThrowException_whenRequiredFieldsNotProvided() {
            context.setConfig(ConfigFactory.empty());

            assertThatThrownBy(() -> factory.instantiate(context, null, ConfigurationRecord.class))
                    .isInstanceOf(EdcInjectionException.class);
        }

        @Test
        void shouldInstantiateRecord() {
            context.setConfig(ConfigFactory.fromMap(Map.of(
                    "required", "requiredValue",
                    "double", "42",
                    "duration", "PT10S"
            )));

            var object = factory.instantiate(context, null, ConfigurationRecord.class);

            assertThat(object).isInstanceOfSatisfying(ConfigurationRecord.class, configuration -> {
                assertThat(configuration.duration()).isEqualTo(Duration.ofSeconds(10));
                assertThat(configuration.doubleValue()).isEqualTo(42);
                assertThat(configuration.duration()).isEqualTo(Duration.ofSeconds(10));
            });
        }

        @Test
        void shouldInstantiateRecordWithPrefix() {
            context.setConfig(ConfigFactory.fromMap(Map.of(
                    "prefix.required", "requiredValue",
                    "prefix.double", "42",
                    "prefix.duration", "PT10S"
            )));

            var object = factory.instantiate(context, "prefix", ConfigurationRecord.class);

            assertThat(object).isInstanceOfSatisfying(ConfigurationRecord.class, configuration -> {
                assertThat(configuration.duration()).isEqualTo(Duration.ofSeconds(10));
                assertThat(configuration.doubleValue()).isEqualTo(42);
                assertThat(configuration.duration()).isEqualTo(Duration.ofSeconds(10));
            });
        }

        @Settings
        public record ConfigurationRecord(@Setting(key = "required") String required,
                                          @Setting(key = "quizz.quazz", required = false) Long optionalVal,
                                          @Setting(key = "test.key2", defaultValue = DEFAULT_VALUE) String requiredValWithDefault,
                                          @Setting(key = "double", defaultValue = DEFAULT_VALUE) Double doubleValue,
                                          @Setting(key = "duration", defaultValue = "PT1S") Duration duration) {
            public static final String DEFAULT_VALUE = "default-value";
        }
    }

    @Nested
    class NestedConfiguration {

        @Test
        void shouldInstantiateRecordWithNestedConfigurationMap() {
            context.setConfig(ConfigFactory.fromMap(Map.of(
                    "required", "requiredValue",
                    "nested.entry1.key", "value1",
                    "nested.entry2.key", "value2"
            )));

            var object = factory.instantiate(context, null, RecordWithNestedMap.class);

            assertThat(object).isInstanceOfSatisfying(RecordWithNestedMap.class, configuration -> {
                assertThat(configuration.required()).isEqualTo("requiredValue");
                assertThat(configuration.nested()).containsKeys("entry1", "entry2");
                assertThat(configuration.nested().get("entry1").key()).isEqualTo("value1");
                assertThat(configuration.nested().get("entry2").key()).isEqualTo("value2");
            });
        }

        @Test
        void shouldInstantiateRecordWithNestedConfigurationMapAndPrefix() {
            context.setConfig(ConfigFactory.fromMap(Map.of(
                    "prefix.required", "requiredValue",
                    "prefix.nested.entry1.key", "value1",
                    "prefix.nested.entry2.key", "value2"
            )));

            var object = factory.instantiate(context, "prefix", RecordWithNestedMap.class);

            assertThat(object).isInstanceOfSatisfying(RecordWithNestedMap.class, configuration -> {
                assertThat(configuration.required()).isEqualTo("requiredValue");
                assertThat(configuration.nested()).containsKeys("entry1", "entry2");
                assertThat(configuration.nested().get("entry1").key()).isEqualTo("value1");
                assertThat(configuration.nested().get("entry2").key()).isEqualTo("value2");
            });
        }

        @Test
        void shouldInstantiateClassWithNestedConfigurationMap() {
            context.setConfig(ConfigFactory.fromMap(Map.of(
                    "required", "requiredValue",
                    "nested.entry1.key", "value1"
            )));

            var object = factory.instantiate(context, null, ClassWithNestedMap.class);

            assertThat(object).isInstanceOfSatisfying(ClassWithNestedMap.class, configuration -> {
                assertThat(configuration.getRequired()).isEqualTo("requiredValue");
                assertThat(configuration.getNested()).containsKey("entry1");
                assertThat(configuration.getNested().get("entry1").key()).isEqualTo("value1");
            });
        }

        @Test
        void shouldThrowException_whenNestedConfigurationContextIsEmpty() {
            context.setConfig(ConfigFactory.fromMap(Map.of("required", "requiredValue")));

            assertThatThrownBy(() -> factory.instantiate(context, null, RecordWithEmptyContextConfig.class))
                    .isInstanceOf(EdcInjectionException.class);
        }

        @Test
        void shouldInstantiateRecordWithNestedConfigurationObject() {
            context.setConfig(ConfigFactory.fromMap(Map.of(
                    "required", "requiredValue",
                    "sub.key", "nestedValue"
            )));

            var object = factory.instantiate(context, null, RecordWithNestedObject.class);

            assertThat(object).isInstanceOfSatisfying(RecordWithNestedObject.class, configuration -> {
                assertThat(configuration.required()).isEqualTo("requiredValue");
                assertThat(configuration.sub().key()).isEqualTo("nestedValue");
            });
        }

        @Settings
        public record RecordWithNestedMap(
                @Setting(key = "required") String required,
                @Configuration(context = "nested") Map<String, NestedEntry> nested
        ) {}

        @Settings
        public record RecordWithNestedObject(
                @Setting(key = "required") String required,
                @Configuration(context = "sub") NestedEntry sub
        ) {}

        @Settings
        public static class ClassWithNestedMap {
            @Setting(key = "required")
            private String required;

            @Configuration(context = "nested")
            private Map<String, NestedEntry> nested;

            public String getRequired() {
                return required;
            }

            public Map<String, NestedEntry> getNested() {
                return nested;
            }
        }

        @Settings
        public record RecordWithEmptyContextConfig(
                @Setting(key = "required") String required,
                @Configuration Map<String, NestedEntry> nested
        ) {}

        @Settings
        public record NestedEntry(@Setting(key = "key") String key) {}
    }

}