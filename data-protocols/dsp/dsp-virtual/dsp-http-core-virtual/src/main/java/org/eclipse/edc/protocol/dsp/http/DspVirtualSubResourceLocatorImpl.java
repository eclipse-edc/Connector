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

package org.eclipse.edc.protocol.dsp.http;

import org.eclipse.edc.protocol.dsp.spi.http.DspVirtualSubResourceLocator;

import java.util.HashMap;
import java.util.Map;

public class DspVirtualSubResourceLocatorImpl implements DspVirtualSubResourceLocator {

    private final Map<String, Map<String, Object>> subResources = new HashMap<>();

    @Override
    public Object getSubResource(String resourceName, String version) {
        var subResource = subResources.get(resourceName);
        if (subResource == null) {
            return null;
        }
        return subResource.get(version);
    }

    @Override
    public void registerSubResource(String resourceName, String version, Object resource) {
        var subResource = subResources.computeIfAbsent(resourceName, (k) -> new HashMap<>());
        subResource.put(version, resource);
    }
}
