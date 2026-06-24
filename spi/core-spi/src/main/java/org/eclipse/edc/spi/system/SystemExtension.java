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

/**
 * An extension that contributes capabilities to the runtime.
 * Subtypes provide specific classes of capabilities such as bootstrap, monitoring, and transport services.
 */
public interface SystemExtension {

    /**
     * Returns the name of the extension, by default is its class name
     */
    default String name() {
        return getClass().getName();
    }

}
