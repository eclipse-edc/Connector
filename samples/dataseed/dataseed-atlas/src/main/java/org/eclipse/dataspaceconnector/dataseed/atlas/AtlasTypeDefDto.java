/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors: 1
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.dataseed.atlas;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.dataspaceconnector.catalog.atlas.metadata.AtlasCustomTypeAttribute;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public class AtlasTypeDefDto {
    @JsonProperty
    private String typeName;
    @JsonProperty
    private Set<String> superTypeNames;
    @JsonProperty
    private List<AtlasCustomTypeAttribute> attributes = new ArrayList<>();

    public String getTypeKeyName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public Set<String> getSuperTypeNames() {
        return superTypeNames;
    }

    public void setSuperTypeNames(Set<String> superTypeNames) {
        this.superTypeNames = superTypeNames;
    }

    public List<AtlasCustomTypeAttribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<AtlasCustomTypeAttribute> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String toString() {
        return "AtlasTypeDefDto{" +
                "typeName='" + typeName + '\'' +
                ", superTypeNames=" + superTypeNames +
                ", attributes=" + attributes +
                '}';
    }

}
