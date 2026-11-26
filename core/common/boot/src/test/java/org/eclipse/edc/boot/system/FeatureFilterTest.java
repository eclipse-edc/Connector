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

import org.eclipse.edc.boot.system.testextensions.AlternativeFeatureExtension;
import org.eclipse.edc.boot.system.testextensions.DefaultFeatureExtension;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.boot.system.TestFunctions.createProviderExtension;
import static org.eclipse.edc.boot.system.TestFunctions.mutableListOf;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FeatureFilterTest {

    private final Monitor monitor = mock();

    @Test
    void noBoundExtensions_shouldReturnAllWithoutReadingConfig() {
        ServiceExtensionContext context = mock();
        var extensions = mutableListOf(createProviderExtension(false), new AnnotatedUnboundExtension());

        var loaded = FeatureFilter.filter(context, extensions);

        assertThat(loaded).containsExactlyElementsOf(extensions);
        verifyNoInteractions(context);
    }

    @Test
    void unboundExtension_isAlwaysLoaded() {
        var notAnnotated = createProviderExtension(false);
        var annotated = new AnnotatedUnboundExtension();

        var loaded = FeatureFilter.filter(context(Map.of(DefaultFeatureExtension.FEATURE_KEY, "alternative")),
                mutableListOf(notAnnotated, annotated, new AlternativeFeatureExtension()));

        assertThat(loaded).contains(notAnnotated, annotated);
    }

    @Test
    void featureKeyNotSet_shouldLoadDefaultOnly() {
        var defaultExtension = new DefaultFeatureExtension();

        var loaded = FeatureFilter.filter(context(Map.of()), mutableListOf(defaultExtension, new AlternativeFeatureExtension()));

        assertThat(loaded).containsExactly(defaultExtension);
    }

    @Test
    void featureKeySet_shouldLoadMatchingExtensionOnly() {
        var alternativeExtension = new AlternativeFeatureExtension();

        var loaded = FeatureFilter.filter(context(Map.of(DefaultFeatureExtension.FEATURE_KEY, "alternative")),
                mutableListOf(new DefaultFeatureExtension(), alternativeExtension));

        assertThat(loaded).containsExactly(alternativeExtension);
    }

    @Test
    void featureKeyNotSet_noDefault_shouldLoadNone() {
        var loaded = FeatureFilter.filter(context(Map.of()), mutableListOf(new AlternativeFeatureExtension()));

        assertThat(loaded).isEmpty();
    }

    @Test
    void featureKeySet_unknownValue_shouldThrow() {
        var context = context(Map.of(DefaultFeatureExtension.FEATURE_KEY, "unknown"));
        var extensions = mutableListOf(new DefaultFeatureExtension(), new AlternativeFeatureExtension());

        assertThatThrownBy(() -> FeatureFilter.filter(context, extensions))
                .isInstanceOf(EdcException.class)
                .hasMessageContaining(DefaultFeatureExtension.FEATURE_KEY)
                .hasMessageContaining("unknown");
    }

    @Test
    void shouldLogFeatureValueAndSkippedExtensions() {
        FeatureFilter.filter(context(Map.of(DefaultFeatureExtension.FEATURE_KEY, "alternative")),
                mutableListOf(new DefaultFeatureExtension(), new AlternativeFeatureExtension()));

        verify(monitor).debug(argThat((String message) -> message.contains(DefaultFeatureExtension.FEATURE_KEY) &&
                message.contains("alternative") && message.contains(DefaultFeatureExtension.NAME)));
    }

    private ServiceExtensionContext context(Map<String, String> settings) {
        ServiceExtensionContext context = mock();
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(settings));
        when(context.getMonitor()).thenReturn(monitor);
        return context;
    }

    @Extension("annotated, not bound to a feature")
    private static class AnnotatedUnboundExtension implements ServiceExtension {
    }
}
