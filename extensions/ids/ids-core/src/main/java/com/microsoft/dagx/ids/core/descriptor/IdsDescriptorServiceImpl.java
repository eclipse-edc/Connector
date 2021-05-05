/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.ids.core.descriptor;

import com.microsoft.dagx.ids.spi.descriptor.IdsDescriptorService;

import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Collections.unmodifiableMap;

/**
 * Provides the IDS self descriptor. Extensions contribute elements to that descriptor.
 */
public class IdsDescriptorServiceImpl implements IdsDescriptorService {
    private LinkedHashMap<String, Object> descriptor = new LinkedHashMap<>();

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
