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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;

@JsonAutoDetect(getterVisibility = PUBLIC_ONLY, setterVisibility = PUBLIC_ONLY, fieldVisibility = NONE)
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AtlasStruct implements Serializable {
    public static final String KEY_TYPENAME = "typeName";
    public static final String KEY_ATTRIBUTES = "attributes";
    public static final String SERIALIZED_DATE_FORMAT_STR = "yyyyMMdd-HH:mm:ss.SSS-Z";
    @Deprecated
    public static final DateFormat DATE_FORMATTER = new SimpleDateFormat(SERIALIZED_DATE_FORMAT_STR);
    private static final long serialVersionUID = 1L;
    private String typeName;
    private Map<String, Object> attributes;

    public AtlasStruct() {
        this(null, null);
    }

    public AtlasStruct(String typeName) {
        this(typeName, null);
    }

    public AtlasStruct(String typeName, Map<String, Object> attributes) {
        setTypeName(typeName);
        setAttributes(attributes);
    }

    public AtlasStruct(String typeName, String attrName, Object attrValue) {
        setTypeName(typeName);
        setAttribute(attrName, attrValue);
    }

    public AtlasStruct(Map map) {
        if (map != null) {
            Object typeName = map.get(KEY_TYPENAME);
            Map attributes = (map.get(KEY_ATTRIBUTES) instanceof Map) ? (Map) map.get(KEY_ATTRIBUTES) : map;

            if (typeName != null) {
                setTypeName(typeName.toString());
            }

            setAttributes(new HashMap<>(attributes));
        }
    }

    public AtlasStruct(AtlasStruct other) {
        if (other != null) {
            setTypeName(other.getTypeName());
            setAttributes(new HashMap<>(other.getAttributes()));
        }
    }

    public static StringBuilder dumpModelObjects(Collection<? extends AtlasStruct> objList, StringBuilder sb) {
        if (sb == null) {
            sb = new StringBuilder();
        }

        if (Functions.isNotEmpty(objList)) {
            int i = 0;
            for (AtlasStruct obj : objList) {
                if (i > 0) {
                    sb.append(", ");
                }

                obj.toString(sb);
                i++;
            }
        }

        return sb;
    }

    public static StringBuilder dumpObjects(Collection<?> objects, StringBuilder sb) {
        if (sb == null) {
            sb = new StringBuilder();
        }

        if (Functions.isNotEmpty(objects)) {
            int i = 0;
            for (Object obj : objects) {
                if (i > 0) {
                    sb.append(", ");
                }

                sb.append(obj);
                i++;
            }
        }

        return sb;
    }

    public static StringBuilder dumpObjects(Map<?, ?> objects, StringBuilder sb) {
        if (sb == null) {
            sb = new StringBuilder();
        }

        if (Functions.isNotEmpty(objects)) {
            int i = 0;
            for (Map.Entry<?, ?> e : objects.entrySet()) {
                if (i > 0) {
                    sb.append(", ");
                }

                sb.append(e.getKey()).append(":").append(e.getValue());
                i++;
            }
        }

        return sb;
    }

    public static StringBuilder dumpDateField(String prefix, Date value, StringBuilder sb) {
        sb.append(prefix);

        if (value == null) {
            sb.append(value);
        } else {
            sb.append(AtlasBaseTypeDef.getDateFormatter().format(value));
        }

        return sb;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public boolean hasAttribute(String name) {
        Map<String, Object> a = attributes;

        return a != null ? a.containsKey(name) : false;
    }

    public Object getAttribute(String name) {
        Map<String, Object> a = attributes;

        return a != null ? a.get(name) : null;
    }

    public void setAttribute(String name, Object value) {
        Map<String, Object> a = attributes;

        if (a != null) {
            a.put(name, value);
        } else {
            a = new HashMap<>();
            a.put(name, value);

            attributes = a;
        }
    }

    public Object removeAttribute(String name) {
        Map<String, Object> a = attributes;

        return a != null ? a.remove(name) : null;
    }

    public StringBuilder toString(StringBuilder sb) {
        if (sb == null) {
            sb = new StringBuilder();
        }

        sb.append("AtlasStruct{");
        sb.append("typeName='").append(typeName).append('\'');
        sb.append(", attributes=[");
        dumpObjects(attributes, sb);
        sb.append("]");
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
        AtlasStruct that = (AtlasStruct) o;

        return Objects.equals(typeName, that.typeName) &&
                Objects.equals(attributes, that.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeName, attributes);
    }

    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }

}
