/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.clients.postgresql.asset.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class Envelope {

    public static final String PROPERTY_NAME_OBJ = "obj";
    public static final String PROPERTY_NAME_CLASS_NAME = "className";

    @JsonProperty(PROPERTY_NAME_OBJ)
    private final Object obj;

    @JsonProperty(PROPERTY_NAME_CLASS_NAME)
    private final String className;

    @JsonCreator
    public Envelope(@NotNull Object obj, @NotNull String className) {
        this.obj = Objects.requireNonNull(obj);
        this.className = Objects.requireNonNull(className);
    }

    public Object getObj() {
        return obj;
    }

    public String getClassName() {
        return className;
    }
}
