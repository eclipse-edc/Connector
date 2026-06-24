/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.spi.query;

/**
 * Extract the property value from an object
 */
@FunctionalInterface
public interface PropertyLookup {

    /**
     * Extract the property value from an object.
     *
     * @param key the key.
     * @param object the object.
     * @return the property value. Null if the property does not exist.
     */
    Object getProperty(String key, Object object);

}
