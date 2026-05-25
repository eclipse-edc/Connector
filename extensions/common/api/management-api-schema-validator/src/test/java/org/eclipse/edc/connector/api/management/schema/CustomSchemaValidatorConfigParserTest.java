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

package org.eclipse.edc.connector.api.management.schema;

import org.eclipse.edc.connector.api.management.schema.CustomSchemaValidatorConfigParser.PrefixMapping;
import org.eclipse.edc.connector.api.management.schema.CustomSchemaValidatorConfigParser.ValidatorBinding;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.connector.api.management.schema.ManagementApiSchemaValidatorExtension.CONFIG_PREFIX;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class CustomSchemaValidatorConfigParserTest {

    private final Monitor monitor = mock(Monitor.class);

    @Test
    void parse_emptyConfig_returnsEmptyList() {
        var config = ConfigFactory.fromMap(Map.of()).getConfig(CONFIG_PREFIX);

        var result = CustomSchemaValidatorConfigParser.parse(config, monitor);

        assertThat(result).isEmpty();
        verifyNoInteractions(monitor);
    }

    @Test
    void parse_singleGroup_withMappingAndMultipleBindings() {
        var config = ConfigFactory.fromMap(Map.of(
                "edc.mgmt.api.schema.custom.version", "v4",
                "edc.mgmt.api.schema.custom.mapping.from", "https://example.org/schema/v4",
                "edc.mgmt.api.schema.custom.mapping.to", "file:///tmp/schema/v4",
                "edc.mgmt.api.schema.custom.validator.policy.type", "PolicyDefinition",
                "edc.mgmt.api.schema.custom.validator.policy.schema", "https://example.org/schema/v4/policy-def-schema.json",
                "edc.mgmt.api.schema.custom.validator.asset.type", "Asset",
                "edc.mgmt.api.schema.custom.validator.asset.schema", "https://example.org/schema/v4/asset-def-schema.json"
        )).getConfig(CONFIG_PREFIX);

        var result = CustomSchemaValidatorConfigParser.parse(config, monitor);

        assertThat(result).hasSize(1);
        var group = result.get(0);
        assertThat(group.groupName()).isEqualTo("custom");
        assertThat(group.version()).isEqualTo("v4");
        assertThat(group.mapping()).isEqualTo(new PrefixMapping("https://example.org/schema/v4", "file:///tmp/schema/v4"));
        assertThat(group.bindings()).containsExactlyInAnyOrder(
                new ValidatorBinding("PolicyDefinition", "https://example.org/schema/v4/policy-def-schema.json"),
                new ValidatorBinding("Asset", "https://example.org/schema/v4/asset-def-schema.json")
        );
        verifyNoInteractions(monitor);
    }

    @Test
    void parse_multipleGroups_independentVersions() {
        var config = ConfigFactory.fromMap(Map.of(
                "edc.mgmt.api.schema.first.version", "v4",
                "edc.mgmt.api.schema.first.validator.policy.type", "PolicyDefinition",
                "edc.mgmt.api.schema.first.validator.policy.schema", "https://example.org/v4/policy.json",
                "edc.mgmt.api.schema.second.version", "v5",
                "edc.mgmt.api.schema.second.validator.asset.type", "Asset",
                "edc.mgmt.api.schema.second.validator.asset.schema", "https://example.org/v5/asset.json"
        )).getConfig(CONFIG_PREFIX);

        var result = CustomSchemaValidatorConfigParser.parse(config, monitor);

        assertThat(result).hasSize(2);
        assertThat(result).anySatisfy(g -> {
            assertThat(g.groupName()).isEqualTo("first");
            assertThat(g.version()).isEqualTo("v4");
            assertThat(g.mapping()).isNull();
            assertThat(g.bindings()).containsExactly(new ValidatorBinding("PolicyDefinition", "https://example.org/v4/policy.json"));
        });
        assertThat(result).anySatisfy(g -> {
            assertThat(g.groupName()).isEqualTo("second");
            assertThat(g.version()).isEqualTo("v5");
            assertThat(g.mapping()).isNull();
            assertThat(g.bindings()).containsExactly(new ValidatorBinding("Asset", "https://example.org/v5/asset.json"));
        });
    }

    @Test
    void parse_missingVersion_throws() {
        var config = ConfigFactory.fromMap(Map.of(
                "edc.mgmt.api.schema.broken.validator.policy.type", "PolicyDefinition",
                "edc.mgmt.api.schema.broken.validator.policy.schema", "https://example.org/policy.json"
        )).getConfig(CONFIG_PREFIX);

        assertThatThrownBy(() -> CustomSchemaValidatorConfigParser.parse(config, monitor))
                .isInstanceOf(EdcException.class)
                .hasMessageContaining("version");
    }

    @Test
    void parse_validatorEntryMissingType_throws() {
        var config = ConfigFactory.fromMap(Map.of(
                "edc.mgmt.api.schema.custom.version", "v4",
                "edc.mgmt.api.schema.custom.validator.policy.schema", "https://example.org/policy.json"
        )).getConfig(CONFIG_PREFIX);

        assertThatThrownBy(() -> CustomSchemaValidatorConfigParser.parse(config, monitor))
                .isInstanceOf(EdcException.class)
                .hasMessageContaining("type");
    }

    @Test
    void parse_validatorEntryMissingSchema_throws() {
        var config = ConfigFactory.fromMap(Map.of(
                "edc.mgmt.api.schema.custom.version", "v4",
                "edc.mgmt.api.schema.custom.validator.policy.type", "PolicyDefinition"
        )).getConfig(CONFIG_PREFIX);

        assertThatThrownBy(() -> CustomSchemaValidatorConfigParser.parse(config, monitor))
                .isInstanceOf(EdcException.class)
                .hasMessageContaining("schema");
    }

    @Test
    void parse_onlyMappingFromSet_warnsAndDropsMapping() {
        var config = ConfigFactory.fromMap(Map.of(
                "edc.mgmt.api.schema.custom.version", "v4",
                "edc.mgmt.api.schema.custom.mapping.from", "https://example.org/schema/v4",
                "edc.mgmt.api.schema.custom.validator.policy.type", "PolicyDefinition",
                "edc.mgmt.api.schema.custom.validator.policy.schema", "https://example.org/policy.json"
        )).getConfig(CONFIG_PREFIX);

        var result = CustomSchemaValidatorConfigParser.parse(config, monitor);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).mapping()).isNull();
        assertThat(result.get(0).bindings()).hasSize(1);
        verify(monitor).warning(org.mockito.ArgumentMatchers.contains("custom"));
    }

    @Test
    void parse_onlyMappingToSet_warnsAndDropsMapping() {
        var config = ConfigFactory.fromMap(Map.of(
                "edc.mgmt.api.schema.custom.version", "v4",
                "edc.mgmt.api.schema.custom.mapping.to", "file:///tmp/schema/v4",
                "edc.mgmt.api.schema.custom.validator.policy.type", "PolicyDefinition",
                "edc.mgmt.api.schema.custom.validator.policy.schema", "https://example.org/policy.json"
        )).getConfig(CONFIG_PREFIX);

        var result = CustomSchemaValidatorConfigParser.parse(config, monitor);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).mapping()).isNull();
        verify(monitor).warning(org.mockito.ArgumentMatchers.contains("custom"));
    }

    @Test
    void parse_groupWithMappingOnly_returnsGroupWithEmptyBindings() {
        var config = ConfigFactory.fromMap(Map.of(
                "edc.mgmt.api.schema.mapper.version", "v4",
                "edc.mgmt.api.schema.mapper.mapping.from", "https://example.org/schema/v4",
                "edc.mgmt.api.schema.mapper.mapping.to", "file:///tmp/schema/v4"
        )).getConfig(CONFIG_PREFIX);

        var result = CustomSchemaValidatorConfigParser.parse(config, monitor);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).bindings()).isEmpty();
        assertThat(result.get(0).mapping()).isEqualTo(new PrefixMapping("https://example.org/schema/v4", "file:///tmp/schema/v4"));
    }
}
