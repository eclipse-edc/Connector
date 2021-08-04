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

package org.eclipse.dataspaceconnector.spi.system;

import org.jetbrains.annotations.Nullable;

/**
 * Contributes configuration to a runtime. Multiple configuration extensions may be loaded in a runtime.
 */
public interface ConfigurationExtension extends BootExtension {

    /**
     * Returns the configuration setting for the key or null if not found.
     */
    @Nullable
    String getSetting(String key);

}
