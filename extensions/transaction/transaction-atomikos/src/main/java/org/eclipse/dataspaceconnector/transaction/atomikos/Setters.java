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

import org.eclipse.dataspaceconnector.spi.EdcException;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.lang.String.format;

/**
 * Helpers for setting Atomikos properties.
 */
public class Setters {

    public static void setMandatory(String key, String name, Consumer<String> setter, Map<String, Object> properties) {
        var value = properties.get(key);
        if (value == null) {
            throw new EdcException(format("Data source key %s not set for %s", key, name));
        }
        setter.accept(value.toString());
    }

    public static void setIfProvided(String key, Consumer<String> setter, Map<String, Object> properties) {
        var value = properties.get(key);
        if (value == null) {
            return;
        }
        setter.accept(value.toString());
    }

    public static void setIfProvided(String key, Consumer<String> setter, ServiceExtensionContext context) {
        var value = context.getSetting(key, null);
        if (value == null) {
            return;
        }
        setter.accept(value);
    }

    public static void setIfProvidedInt(String key, String name, Consumer<Integer> setter, ServiceExtensionContext context) {
        setIfProvidedInt(key, name, setter, () -> context.getSetting(key, null));
    }

    public static void setIfProvidedInt(String key, String name, Consumer<Integer> setter, Map<String, Object> properties) {
        setIfProvidedInt(key, name, setter, () -> {
            var rawProperty = properties.get(key);
            return rawProperty == null ? null : rawProperty.toString();
        });
    }

    public static void setIfProvidedInt(String key, String name, Consumer<Integer> setter, Supplier<String> supplier) {
        var value = supplier.get();
        if (value == null) {
            return;
        }
        try {
            var parsed = Integer.parseInt(value);
            if (parsed == -1) {
                return; // not set
            }
            setter.accept(parsed);
        } catch (NumberFormatException e) {
            throw new EdcException(format("Error configuring %s. Value must be an integer for for %s: %s", name, key, value));
        }
    }


}
