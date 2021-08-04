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

package org.eclipse.dataspaceconnector.ids.core.descriptor;

import org.eclipse.dataspaceconnector.ids.spi.descriptor.IdsDescriptorService;

import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Collections.unmodifiableMap;

/**
 * Provides the IDS self descriptor. Extensions contribute elements to that descriptor.
 */
public class IdsDescriptorServiceImpl implements IdsDescriptorService {
    private final LinkedHashMap<String, Object> descriptor = new LinkedHashMap<>();

    public IdsDescriptorServiceImpl() {
        LinkedHashMap<String, String> description = new LinkedHashMap<>();
        description.put("@value", "Self Description");
        description.put("@language", "en");
        descriptor.put("ids:description", description);
    }

    /**
     * Registration should only be performed at startup and not after.
     */
    @Override
    public void registerDescriptorElement(Object element) {

    }

    @Override
    public Map<String, Object> description() {
        return unmodifiableMap(descriptor);
    }
}
