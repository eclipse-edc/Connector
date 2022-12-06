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

package org.eclipse.edc.spi.system;

import org.eclipse.edc.spi.monitor.Monitor;

/**
 * Contributes capabilities and services
 */
public interface BootExtension extends SystemExtension {

    /**
     * Initializes the extension.
     *
     * @param monitor The {@link Monitor instance}
     */
    default void initialize(Monitor monitor) {
    }

}
