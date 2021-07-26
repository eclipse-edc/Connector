/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.catalog.atlas.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;

@JsonAutoDetect(getterVisibility = PUBLIC_ONLY, setterVisibility = PUBLIC_ONLY, fieldVisibility = NONE)
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AtlasBusinessMetadataDef extends AtlasStructDef implements Serializable {
    public static final String ATTR_OPTION_APPLICABLE_ENTITY_TYPES = "applicableEntityTypes";
    public static final String ATTR_MAX_STRING_LENGTH = "maxStrLength";
    public static final String ATTR_VALID_PATTERN = "validPattern";
    private static final long serialVersionUID = 1L;

    public AtlasBusinessMetadataDef() {
        this(null, null, null, null);
    }

    public AtlasBusinessMetadataDef(String name, String description) {
        this(name, description, null, null, null);
    }

    public AtlasBusinessMetadataDef(String name, String description, String typeVersion) {
        this(name, description, typeVersion, null, null);
    }

    public AtlasBusinessMetadataDef(String name, String description, String typeVersion, List<AtlasAttributeDef> attributeDefs) {
        this(name, description, typeVersion, attributeDefs, null);
    }

    public AtlasBusinessMetadataDef(String name, String description, String typeVersion, List<AtlasAttributeDef> attributeDefs, Map<String, String> options) {
        super(TypeCategory.BUSINESS_METADATA, name, description, typeVersion, attributeDefs, options);
    }

    public AtlasBusinessMetadataDef(AtlasBusinessMetadataDef other) {
        super(other);
    }

    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }

    @Override
    public StringBuilder toString(StringBuilder sb) {
        if (sb == null) {
            sb = new StringBuilder();
        }

        sb.append("AtlasBusinessMetadataDef{");
        super.toString(sb);
        sb.append('}');

        return sb;
    }
}
