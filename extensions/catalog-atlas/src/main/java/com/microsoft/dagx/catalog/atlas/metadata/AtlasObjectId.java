/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.catalog.atlas.metadata;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.microsoft.dagx.catalog.atlas.dto.AtlasBaseTypeDef;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;

@JsonAutoDetect(getterVisibility = PUBLIC_ONLY, setterVisibility = PUBLIC_ONLY, fieldVisibility = NONE)
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AtlasObjectId implements Serializable {
    public static final String KEY_GUID = "guid";
    public static final String KEY_TYPENAME = "typeName";
    public static final String KEY_UNIQUE_ATTRIBUTES = "uniqueAttributes";
    private static final long serialVersionUID = 1L;
    private String guid;
    private String typeName;
    private Map<String, Object> uniqueAttributes;


    public AtlasObjectId() {
        this(null, null, (Map<String, Object>) null);
    }

    public AtlasObjectId(String guid) {
        this(guid, null, (Map<String, Object>) null);
    }

    public AtlasObjectId(String guid, String typeName) {
        this(guid, typeName, (Map<String, Object>) null);
    }

    public AtlasObjectId(String typeName, Map<String, Object> uniqueAttributes) {
        this(null, typeName, uniqueAttributes);
    }

    public AtlasObjectId(String typeName, final String attrName, final Object attrValue) {
        this(null, typeName, new HashMap<>() {{
            put(attrName, attrValue);
        }});
    }

    public AtlasObjectId(String guid, String typeName, Map<String, Object> uniqueAttributes) {
        setGuid(guid);
        setTypeName(typeName);
        setUniqueAttributes(uniqueAttributes);
    }

    public AtlasObjectId(AtlasObjectId other) {
        if (other != null) {
            setGuid(other.getGuid());
            setTypeName(other.getTypeName());
            setUniqueAttributes(other.getUniqueAttributes());
        }
    }

    public AtlasObjectId(Map objIdMap) {
        if (objIdMap != null) {
            Object g = objIdMap.get(KEY_GUID);
            Object t = objIdMap.get(KEY_TYPENAME);
            Object u = objIdMap.get(KEY_UNIQUE_ATTRIBUTES);

            if (g != null) {
                setGuid(g.toString());
            }

            if (t != null) {
                setTypeName(t.toString());
            }

            if (u != null && u instanceof Map) {
                setUniqueAttributes((Map) u);
            }
        }
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public Map<String, Object> getUniqueAttributes() {
        return uniqueAttributes;
    }

    public void setUniqueAttributes(Map<String, Object> uniqueAttributes) {
        this.uniqueAttributes = uniqueAttributes;
    }

    public StringBuilder toString(StringBuilder sb) {
        if (sb == null) {
            sb = new StringBuilder();
        }

        sb.append("AtlasObjectId{");
        sb.append("guid='").append(guid).append('\'');
        sb.append(", typeName='").append(typeName).append('\'');
        sb.append(", uniqueAttributes={");
        AtlasBaseTypeDef.dumpObjects(uniqueAttributes, sb);
        sb.append('}');
        sb.append('}');

        return sb;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AtlasObjectId that = (AtlasObjectId) o;

        // if guid is empty/null, equality should be based on typeName/uniqueAttributes
        if (guid == null && that.guid == null) {
            return Objects.equals(typeName, that.typeName) && Objects.equals(uniqueAttributes, that.uniqueAttributes);
        } else {
            return Objects.equals(guid, that.guid);
        }
    }

    @Override
    public int hashCode() {
        return guid != null ? Objects.hash(guid) : Objects.hash(typeName, uniqueAttributes);
    }

    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }

}
