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

package org.eclipse.dataspaceconnector.ids.spi.descriptor;

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
