/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package org.eclipse.edc.ids.spi.descriptor;

import java.util.Map;

/**
 * Manages the IDS self-descriptor.
 */
public interface IdsDescriptorService {

    /**
     * Registers an element.
     */
    void registerDescriptorElement(Object element);

    /**
     * Returns the description.
     */
    Map<String, Object> description();

}
