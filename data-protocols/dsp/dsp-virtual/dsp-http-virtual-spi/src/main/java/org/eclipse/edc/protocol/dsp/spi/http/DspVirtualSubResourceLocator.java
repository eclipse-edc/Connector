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

package org.eclipse.edc.protocol.dsp.spi.http;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;

/**
 * Extension point for locating sub-resources of a DSP virtual profile resource.
 * <p>
 * The sub-resources are identified by name, and can be registered at runtime. This allows to decouple the
 * profile resource from its sub-resources, and to register them in a flexible way.
 */
@ExtensionPoint
public interface DspVirtualSubResourceLocator {

    /**
     * Returns the sub-resource associated with the given name, or null if no such sub-resource is registered.
     *
     * @param resourceName the name of the sub-resource
     * @return the sub-resource associated with the given name, or null if no such sub-resource is registered
     */
    Object getSubResource(String resourceName, String version);


    /**
     * Registers a sub-resource with the given name. If a sub-resource with the same name is already registered, it will be overwritten.
     *
     * @param resourceName the name of the sub-resource
     * @param resource     the sub-resource to register
     */
    void registerSubResource(String resourceName, String version, Object resource);
}
