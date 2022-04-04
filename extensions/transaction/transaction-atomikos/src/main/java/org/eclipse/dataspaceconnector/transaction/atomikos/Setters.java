/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.transaction.atomikos;

import org.eclipse.dataspaceconnector.spi.system.configuration.Config;

import java.util.function.Consumer;

/**
 * Helpers for setting Atomikos properties.
 */
public class Setters {

    public static void setIfProvided(String key, Consumer<String> setter, Config config) {
        var value = config.getString(key, null);
        if (value == null) {
            return;
        }
        setter.accept(value);
    }

    public static void setIfProvidedInt(String key, Consumer<Integer> setter, Config config) {
        var value = config.getInteger(key, null);
        if (value == null) {
            return;
        }

        setter.accept(value);
    }

}
