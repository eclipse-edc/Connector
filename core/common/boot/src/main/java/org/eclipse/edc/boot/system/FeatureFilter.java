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

package org.eclipse.edc.boot.system;

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import static java.util.stream.Collectors.toCollection;

/**
 * Removes all {@link ServiceExtension}s that are bound to a feature (see {@link Extension#featureKey()}) which is not
 * selected by the runtime configuration. The rules are:
 * <ul>
 *     <li>an extension that is not bound to a feature is always loaded</li>
 *     <li>an extension that is bound to a feature is loaded if the feature key is configured with exactly its {@link Extension#featureValue()}</li>
 *     <li>if the feature key is not configured, the extension is loaded only if {@link Extension#loadAsDefault()} is {@code true}</li>
 *     <li>if a feature key is configured with a value that no extension on the classpath declares, an {@link EdcException} is thrown</li>
 * </ul>
 */
class FeatureFilter {

    private FeatureFilter() {
    }

    /**
     * Returns the extensions that should be loaded given the configuration of the context, preserving their order.
     *
     * @param context    the context, used to resolve the feature keys and to log the outcome.
     * @param extensions all extensions found on the classpath.
     * @return the extensions that should be loaded.
     * @throws EdcException if a feature key is configured with a value that no extension declares.
     */
    static List<ServiceExtension> filter(ServiceExtensionContext context, List<ServiceExtension> extensions) {
        // feature key -> extensions bound to it, in classpath order
        var boundExtensions = new LinkedHashMap<String, List<ServiceExtension>>();
        for (var extension : extensions) {
            var annotation = isBoundByFeatureOrNull(extension);
            if (annotation != null) {
                boundExtensions.computeIfAbsent(annotation.featureKey(), k -> new ArrayList<>()).add(extension);
            }
        }

        if (boundExtensions.isEmpty()) {
            return extensions;
        }

        var config = context.getConfig();
        var selectedValues = new LinkedHashMap<String, String>();
        boundExtensions.forEach((featureKey, bound) -> {
            var value = config.getString(featureKey, null);
            if (value != null && bound.stream().noneMatch(ext -> value.equals(isBoundByFeatureOrNull(ext).featureValue()))) {
                var knownValues = bound.stream().map(ext -> isBoundByFeatureOrNull(ext).featureValue()).collect(toCollection(TreeSet::new));
                throw new EdcException("Feature '%s' is configured with value '%s', but no extension declares this value. Known values: %s"
                        .formatted(featureKey, value, knownValues));
            }
            selectedValues.put(featureKey, value);
        });

        var skipped = new LinkedHashMap<String, List<String>>();
        var loaded = extensions.stream()
                .filter(extension -> {
                    var annotation = isBoundByFeatureOrNull(extension);
                    if (annotation == null) {
                        return true;
                    }
                    var value = selectedValues.get(annotation.featureKey());
                    var load = value == null ? annotation.loadAsDefault() : value.equals(annotation.featureValue());
                    if (!load) {
                        skipped.computeIfAbsent(annotation.featureKey(), k -> new ArrayList<>()).add(annotation.value());
                    }
                    return load;
                })
                .collect(toCollection(ArrayList::new));

        logFeatures(context, selectedValues, skipped);
        return loaded;
    }

    private static void logFeatures(ServiceExtensionContext context, Map<String, String> selectedValues, Map<String, List<String>> skipped) {
        var monitor = context.getMonitor();
        selectedValues.forEach((featureKey, value) -> {
            var valueDescription = value == null ? "not set, using defaults" : "'%s'".formatted(value);
            var skippedExtensions = skipped.getOrDefault(featureKey, List.of());
            var skippedDescription = skippedExtensions.isEmpty()
                    ? "no extensions skipped"
                    : "skipped extensions: " + String.join(", ", skippedExtensions);
            monitor.debug("Feature '%s': %s, %s".formatted(featureKey, valueDescription, skippedDescription));
        });
    }

    /**
     * Returns the {@link Extension} annotation of the extension if it binds it to a feature, {@code null} otherwise.
     */
    private static @Nullable Extension isBoundByFeatureOrNull(ServiceExtension extension) {
        var annotation = extension.getClass().getAnnotation(Extension.class);
        if (annotation == null || annotation.featureKey().isBlank()) {
            return null;
        }
        return annotation;
    }
}
