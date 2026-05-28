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

import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.configuration.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.eclipse.edc.connector.api.management.schema.ManagementApiSchemaValidatorExtension.MAPPING_FROM_KEY;
import static org.eclipse.edc.connector.api.management.schema.ManagementApiSchemaValidatorExtension.MAPPING_TO_KEY;
import static org.eclipse.edc.connector.api.management.schema.ManagementApiSchemaValidatorExtension.VALIDATOR_KEY;
import static org.eclipse.edc.connector.api.management.schema.ManagementApiSchemaValidatorExtension.VALIDATOR_PROFILES_KEY;
import static org.eclipse.edc.connector.api.management.schema.ManagementApiSchemaValidatorExtension.VALIDATOR_SCHEMA_KEY;
import static org.eclipse.edc.connector.api.management.schema.ManagementApiSchemaValidatorExtension.VALIDATOR_TYPE_KEY;
import static org.eclipse.edc.connector.api.management.schema.ManagementApiSchemaValidatorExtension.VERSION_KEY;

public final class CustomSchemaValidatorConfigParser {

    private CustomSchemaValidatorConfigParser() {
    }

    public static List<CustomValidatorGroup> parse(Config root, Monitor monitor) {
        return root.partition()
                .map(groupConfig -> parseGroup(groupConfig, monitor))
                .toList();
    }

    private static CustomValidatorGroup parseGroup(Config groupConfig, Monitor monitor) {
        var groupName = groupConfig.currentNode();
        var version = groupConfig.getString(VERSION_KEY);
        var mapping = parseMapping(groupConfig, groupName, monitor);
        var bindings = groupConfig.getConfig(VALIDATOR_KEY).partition()
                .map(CustomSchemaValidatorConfigParser::parseBinding)
                .toList();
        return new CustomValidatorGroup(groupName, version, mapping, bindings);
    }

    private static PrefixMapping parseMapping(Config groupConfig, String groupName, Monitor monitor) {
        var from = groupConfig.getString(MAPPING_FROM_KEY, null);
        var to = groupConfig.getString(MAPPING_TO_KEY, null);
        if (from == null && to == null) {
            return null;
        }
        if (from == null || to == null) {
            monitor.warning("Custom schema validator group '%s' has only one of '%s'/'%s' set; ignoring mapping."
                    .formatted(groupName, MAPPING_FROM_KEY, MAPPING_TO_KEY));
            return null;
        }
        return new PrefixMapping(from, to);
    }

    private static ValidatorBinding parseBinding(Config entryConfig) {
        var profiles = new ArrayList<String>();
        var rawProfiles = entryConfig.getString(VALIDATOR_PROFILES_KEY, null);
        if (rawProfiles != null) {
            Arrays.stream(rawProfiles.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .forEach(profiles::add);
        }
        return new ValidatorBinding(entryConfig.getString(VALIDATOR_TYPE_KEY), entryConfig.getString(VALIDATOR_SCHEMA_KEY), profiles);
    }

    public record PrefixMapping(String from, String to) {
    }

    public record ValidatorBinding(String type, String schema, List<String> profiles) {
    }

    public record CustomValidatorGroup(String groupName, String version, PrefixMapping mapping,
                                       List<ValidatorBinding> bindings) {
    }

}
