/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.edc.boot.system;

import org.eclipse.edc.boot.system.injection.InjectionContainer;
import org.eclipse.edc.boot.system.testextensions.AlternativeFeatureExtension;
import org.eclipse.edc.boot.system.testextensions.DefaultFeatureExtension;
import org.eclipse.edc.boot.system.testextensions.RequiredDependentExtension;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.boot.system.TestFunctions.createDependentExtension;
import static org.eclipse.edc.boot.system.TestFunctions.createProviderExtension;
import static org.eclipse.edc.boot.system.TestFunctions.mutableListOf;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DependencyGraphTest {


    @Test
    void getExtensions_withDefaultProvider() {
        var providerExtension = createProviderExtension(true);
        var dependentExtension = createDependentExtension(true);

        var list = DependencyGraph.of(mock(), mutableListOf(dependentExtension, providerExtension)).getInjectionContainers();

        assertThat(list).extracting(InjectionContainer::getInjectionTarget)
                .containsExactly(providerExtension, dependentExtension);
    }

    @Test
    void getExtensions_withNoDefaultProvider() {
        var defaultProvider = createProviderExtension(false);
        var provider = createProviderExtension(true);
        var dependentExtension = createDependentExtension(true);

        var list = DependencyGraph.of(mock(), mutableListOf(dependentExtension, provider, defaultProvider)).getInjectionContainers();

        assertThat(list).extracting(InjectionContainer::getInjectionTarget)
                .containsExactly(provider, defaultProvider, dependentExtension);
    }

    @Test
    void getExtensions_missingDependency() {
        var dependentExtension = createDependentExtension(true);

        assertThat(DependencyGraph.of(mock(), mutableListOf(dependentExtension)).isValid()).isFalse();
    }

    @Test
    void getExtensions_missingOptionalDependency() {
        var dependentExtension = createDependentExtension(false);

        var dependencyGraph = DependencyGraph.of(mock(), mutableListOf(dependentExtension));

        assertThat(dependencyGraph.isValid()).isTrue();
        assertThat(dependencyGraph.getInjectionContainers()).hasSize(1)
                .extracting(InjectionContainer::getInjectionTarget)
                .containsExactly(dependentExtension);
    }

    @Nested
    class FeatureBinding {

        private final Monitor monitor = mock();

        @Test
        void unboundExtension_isAlwaysLoaded() {
            var notAnnotated = createProviderExtension(false);
            var annotated = new AnnotatedUnboundExtension();

            var graph = DependencyGraph.of(context(Map.of(DefaultFeatureExtension.FEATURE_KEY, "alternative")),
                    mutableListOf(notAnnotated, annotated, new AlternativeFeatureExtension()));

            assertThat(graph.getExtensions()).contains(notAnnotated, annotated);
        }

        @Test
        void featureKeyNotSet_shouldLoadDefaultOnly() {
            var defaultExtension = new DefaultFeatureExtension();
            var alternativeExtension = new AlternativeFeatureExtension();
            var dependent = new RequiredDependentExtension();

            var graph = DependencyGraph.of(context(Map.of()), mutableListOf(dependent, defaultExtension, alternativeExtension));

            assertThat(graph.isValid()).isTrue();
            assertThat(graph.getExtensions()).containsExactly(defaultExtension, dependent);
        }

        @Test
        void featureKeySet_shouldLoadMatchingExtensionOnly() {
            var defaultExtension = new DefaultFeatureExtension();
            var alternativeExtension = new AlternativeFeatureExtension();
            var dependent = new RequiredDependentExtension();

            var graph = DependencyGraph.of(context(Map.of(DefaultFeatureExtension.FEATURE_KEY, "alternative")),
                    mutableListOf(dependent, defaultExtension, alternativeExtension));

            assertThat(graph.isValid()).isTrue();
            assertThat(graph.getExtensions()).containsExactly(alternativeExtension, dependent);
        }

        @Test
        void featureKeyNotSet_noDefault_shouldNotProvideServices() {
            var dependent = new RequiredDependentExtension();

            var graph = DependencyGraph.of(context(Map.of()), mutableListOf(dependent, new AlternativeFeatureExtension()));

            assertThat(graph.isValid()).isFalse();
            assertThat(graph.getExtensions()).containsExactly(dependent);
        }

        @Test
        void featureKeySet_unknownValue_shouldThrow() {
            var context = context(Map.of(DefaultFeatureExtension.FEATURE_KEY, "unknown"));
            var extensions = mutableListOf(new DefaultFeatureExtension(), new AlternativeFeatureExtension());

            assertThatThrownBy(() -> DependencyGraph.of(context, extensions))
                    .isInstanceOf(EdcException.class)
                    .hasMessageContaining(DefaultFeatureExtension.FEATURE_KEY)
                    .hasMessageContaining("unknown");
        }

        @Test
        void shouldLogFeatureValueAndSkippedExtensions() {
            DependencyGraph.of(context(Map.of(DefaultFeatureExtension.FEATURE_KEY, "alternative")),
                    mutableListOf(new DefaultFeatureExtension(), new AlternativeFeatureExtension()));

            verify(monitor).debug(argThat((String message) -> message.contains(DefaultFeatureExtension.FEATURE_KEY) &&
                    message.contains("alternative") && message.contains(DefaultFeatureExtension.NAME)));
        }

        @Extension("annotated, not bound to a feature")
        private static class AnnotatedUnboundExtension implements ServiceExtension {
        }

        private ServiceExtensionContext context(Map<String, String> settings) {
            ServiceExtensionContext context = mock();
            when(context.getConfig()).thenReturn(ConfigFactory.fromMap(settings));
            when(context.getMonitor()).thenReturn(monitor);
            return context;
        }
    }
}
