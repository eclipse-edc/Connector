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

package org.eclipse.edc.connector.core.store;

import org.eclipse.edc.spi.query.PropertyLookup;
import org.eclipse.edc.util.reflection.ReflectionException;
import org.eclipse.edc.util.reflection.ReflectionUtil;

public class ReflectionPropertyLookup implements PropertyLookup {
    @Override
    public Object getProperty(String key, Object object) {
        try {
            return ReflectionUtil.getFieldValue(key, object);
        } catch (ReflectionException e) {
            return null;
        }
    }
}
