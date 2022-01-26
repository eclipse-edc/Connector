/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.common.configuration;

import org.eclipse.dataspaceconnector.common.string.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Common configuration functions used by extensions.
 */
public class ConfigurationFunctions {

    /**
     * Returns the property value, env value or default value for the key.
     * <p>Naming conventions for keys are '[qualifier].[value]' in lower case. When checking for env variables, keys will be converted to uppercase and '.' replaced by '_'.</p>
     */
    public static String propOrEnv(String key, String defaultValue) {
        String value = System.getProperty(key);
        if (value != null) {
            return value;
        }
        String upperKey = key.toUpperCase().replace('.', '_');
        value = System.getenv(upperKey);
        return value != null ? value : defaultValue;
    }

    /**
     * Takes a list of flat properties and converts them into a hierarchical map of entries. The root node is determined automatically
     * by computing the largest common prefix
     *
     * @param properties A flat map of properties
     * @return A "map of maps", each of which containing the sub-properties of one node.
     * @see ConfigurationFunctions#hierarchical(Map, String)
     * @see StringUtils#getCommonPrefix(List)
     */
    public static Map<String, Map<String, Object>> hierarchical(Map<String, Object> properties) {
        return hierarchical(properties, autoRoot(properties.keySet()));
    }

    private static String autoRoot(Set<String> keySet) {
        return StringUtils.getCommonPrefix(new ArrayList<>(keySet));
    }

    /**
     * Takes a list of flat properties and converts them into a hierarchical map of entries starting from a given root.
     * Ideally, the root should be the common property key prefix. For example lets assume we have those properties:
     * <pre>
     * {@code
     * edc.datasource.default.user=user1
     * edc.datasource.default.password=user1
     * edc.datasource.another.user=user2
     * edc.datasource.another.password=password2
     * }
     * </pre>
     * <p>
     * This would give the following results:
     * {@code
     * hierarchical(props, "edc.datasource") returns "default" -> {"user" -> "user1", "password"->"password1"}
     * "another" -> {"user" -> "user2", "password"->"password2"}
     * }
     * <p>
     * This could be used to dynamically configure a multiple of something, e.g. datasources.
     *
     * @param properties a flat map of properties
     * @param root       The common prefix that is used as starting point
     * @return A "map of maps", each of which containing the sub-properties of one node.
     */
    public static Map<String, Map<String, Object>> hierarchical(Map<String, Object> properties, String root) {
        Objects.requireNonNull(properties, "properties");
        if (root != null) {
            properties = properties.entrySet().stream()
                    .filter(e -> e.getKey().startsWith(root))
                    .collect(Collectors.toMap(s -> s.getKey().replace(root + ".", ""), Map.Entry::getValue));

        }
        Map<String, Map<String, Object>> groupedList = new HashMap<>();
        properties.forEach((k, v) -> {
            if (k.contains(".")) {
                var group = k.split("\\.")[0];
                var key = k.replace(group + ".", "");
                if (groupedList.containsKey(group)) {
                    groupedList.get(group).put(key, v);
                } else {
                    var m = new HashMap<String, Object>();
                    m.put(key, v);
                    groupedList.put(group, m);
                }
            } else {
                var m = groupedList.computeIfAbsent(root, s -> new HashMap<>());
                m.put(k, v);
            }
        });

        return groupedList;
    }
}
