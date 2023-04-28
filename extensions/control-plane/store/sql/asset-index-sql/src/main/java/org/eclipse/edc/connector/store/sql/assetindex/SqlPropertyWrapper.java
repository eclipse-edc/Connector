/*
 *  Copyright (c) 2023 ZF Friedrichshafen AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       ZF Friedrichshafen AG - Initial Implementation
 *
 */

package org.eclipse.edc.connector.store.sql.assetindex;

import java.util.AbstractMap;

public class SqlPropertyWrapper {
    private final boolean isPrivate;
    private final AbstractMap.SimpleImmutableEntry<String, Object> property;

    protected SqlPropertyWrapper(boolean isPrivate, AbstractMap.SimpleImmutableEntry<String, Object> kvSimpleImmutableEntry) {
        this.isPrivate = isPrivate;
        this.property = kvSimpleImmutableEntry;
    }

    protected boolean isPrivate() {
        return isPrivate;
    }

    protected AbstractMap.SimpleImmutableEntry<String, Object> getProperty() {
        return property;
    }

    protected String getPropertyKey() {
        return property.getKey();
    }

    protected Object getPropertyValue() {
        return property.getValue();
    }
}
