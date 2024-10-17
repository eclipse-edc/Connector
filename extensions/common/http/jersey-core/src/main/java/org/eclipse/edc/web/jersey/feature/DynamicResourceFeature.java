/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.web.jersey.feature;

import jakarta.ws.rs.container.DynamicFeature;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.FeatureContext;

import java.util.List;
import java.util.Map;

import static java.util.Optional.ofNullable;

/**
 * A {@link DynamicFeature}  that registers dynamic resources for a given resource class.
 */
public class DynamicResourceFeature implements DynamicFeature {

    private final Map<Class<?>, List<Object>> dynamicResources;

    public DynamicResourceFeature(Map<Class<?>, List<Object>> dynamicResources) {
        this.dynamicResources = dynamicResources;
    }

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        ofNullable(dynamicResources.get(resourceInfo.getResourceClass()))
                .orElse(List.of())
                .forEach(context::register);
    }
}
