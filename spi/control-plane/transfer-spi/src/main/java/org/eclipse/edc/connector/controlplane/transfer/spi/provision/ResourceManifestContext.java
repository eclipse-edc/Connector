/*
 *  Copyright (c) 2022 Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - initial API and implementation
 *
 */

package org.eclipse.edc.connector.controlplane.transfer.spi.provision;

import org.eclipse.edc.connector.controlplane.transfer.spi.types.ResourceDefinition;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.ResourceManifest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides access to a resource manifest's resource definitions by definition type.
 */
public class ResourceManifestContext {

    private Map<Class, List<ResourceDefinition>> resourceDefinitions = new HashMap<>();

    public ResourceManifestContext(ResourceManifest resourceManifest) {
        resourceManifest.getDefinitions().forEach(this::addDefinition);
    }

    /**
     * Adds a resource definition.
     *
     * @param definition the resource definition.
     * @param <T> the type of resource definition.
     */
    public <T extends ResourceDefinition> void addDefinition(T definition) {
        resourceDefinitions.computeIfAbsent(definition.getClass(), c -> new ArrayList<>()).add(definition);
    }

    /**
     * Replaces all definitions of a given type with the given definitions of the same type.
     *
     * @param type the type.
     * @param definitions the list of resource definitions.
     * @param <T> the type of resource definition.
     */
    public <T extends ResourceDefinition> void replaceDefinitions(Class<T> type, List<T> definitions) {
        this.resourceDefinitions.put(type, (List<ResourceDefinition>) definitions);
    }

    /**
     * Returns all resource definitions for the given type.
     *
     * @param type the type.
     * @param <T> the type of resource definition.
     * @return a list of resource definitions.
     */
    public <T extends ResourceDefinition> List<T> getDefinitions(Class<T> type) {
        return (List<T>) resourceDefinitions.get(type);
    }

    /**
     * Returns all resource definitions.
     *
     * @return a list of resource definitions.
     */
    public List<ResourceDefinition> getDefinitions() {
        var list = new ArrayList<ResourceDefinition>();
        resourceDefinitions.values().forEach(list::addAll);
        return list;
    }

}
