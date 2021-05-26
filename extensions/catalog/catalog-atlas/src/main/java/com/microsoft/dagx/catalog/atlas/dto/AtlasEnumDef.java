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
import java.util.*;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;

@JsonAutoDetect(getterVisibility = PUBLIC_ONLY, setterVisibility = PUBLIC_ONLY, fieldVisibility = NONE)
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AtlasEnumDef extends AtlasBaseTypeDef implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<AtlasEnumDef.AtlasEnumElementDef> elementDefs;
    private String defaultValue;

    public AtlasEnumDef() {
        this(null, null, null, null, null, null);
    }

    public AtlasEnumDef(String name) {
        this(name, null, null, null, null, null);
    }

    public AtlasEnumDef(String name, String description) {
        this(name, description, null, null, null, null);
    }

    public AtlasEnumDef(String name, String description, String typeVersion) {
        this(name, description, typeVersion, null, null, null);
    }

    public AtlasEnumDef(String name, String description, List<AtlasEnumDef.AtlasEnumElementDef> elementDefs) {
        this(name, description, null, elementDefs, null, null);
    }

    public AtlasEnumDef(String name, String description, String typeVersion, List<AtlasEnumDef.AtlasEnumElementDef> elementDefs) {
        this(name, description, typeVersion, elementDefs, null, null);
    }

    public AtlasEnumDef(String name, String description, String typeVersion, List<AtlasEnumDef.AtlasEnumElementDef> elementDefs,
                        String defaultValue) {
        this(name, description, typeVersion, elementDefs, defaultValue, null);
    }

    public AtlasEnumDef(String name, String description, String typeVersion, List<AtlasEnumDef.AtlasEnumElementDef> elementDefs,
                        String defaultValue, Map<String, String> options) {
        this(name, description, typeVersion, elementDefs, defaultValue, null, options);
    }

    public AtlasEnumDef(String name, String description, String typeVersion, List<AtlasEnumDef.AtlasEnumElementDef> elementDefs,
                        String defaultValue, String serviceType, Map<String, String> options) {
        super(TypeCategory.ENUM, name, description, typeVersion, serviceType, options);

        setElementDefs(elementDefs);
        setDefaultValue(defaultValue);
    }

    public AtlasEnumDef(AtlasEnumDef other) {
        super(other);

        if (other != null) {
            setElementDefs(other.getElementDefs());
            setDefaultValue(other.getDefaultValue());
        }
    }

    private static boolean hasElement(List<AtlasEnumDef.AtlasEnumElementDef> elementDefs, String elemValue) {
        return findElement(elementDefs, elemValue) != null;
    }

    private static AtlasEnumDef.AtlasEnumElementDef findElement(List<AtlasEnumDef.AtlasEnumElementDef> elementDefs, String elemValue) {
        AtlasEnumDef.AtlasEnumElementDef ret = null;

        if (!elementDefs.isEmpty()) {
            for (AtlasEnumDef.AtlasEnumElementDef elementDef : elementDefs) {
                if (elementDef.getValue().equalsIgnoreCase(elemValue)) {
                    ret = elementDef;
                    break;
                }
            }
        }

        return ret;
    }

    public List<AtlasEnumDef.AtlasEnumElementDef> getElementDefs() {
        return elementDefs;
    }

    public void setElementDefs(List<AtlasEnumDef.AtlasEnumElementDef> elementDefs) {
        if (elementDefs != null && this.elementDefs == elementDefs) {
            return;
        }

        if (elementDefs == null || elementDefs.isEmpty()) {
            this.elementDefs = new ArrayList<>();
        } else {
            // if multiple elements with same value are present, keep only the last entry
            List<AtlasEnumDef.AtlasEnumElementDef> tmpList = new ArrayList<>(elementDefs.size());
            Set<String> elementValues = new HashSet<>();

            ListIterator<AtlasEnumDef.AtlasEnumElementDef> iter = elementDefs.listIterator(elementDefs.size());
            while (iter.hasPrevious()) {
                AtlasEnumDef.AtlasEnumElementDef elementDef = iter.previous();
                String elementValue = elementDef != null ? elementDef.getValue() : null;

                if (elementValue != null) {
                    elementValue = elementValue.toLowerCase();

                    if (!elementValues.contains(elementValue)) {
                        tmpList.add(new AtlasEnumDef.AtlasEnumElementDef(elementDef));

                        elementValues.add(elementValue);
                    }
                }
            }
            Collections.reverse(tmpList);

            this.elementDefs = tmpList;
        }
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String value) {
        defaultValue = value;
    }

    public AtlasEnumDef.AtlasEnumElementDef getElement(String elemValue) {
        return findElement(elementDefs, elemValue);
    }

    public void addElement(AtlasEnumDef.AtlasEnumElementDef elementDef) {
        List<AtlasEnumDef.AtlasEnumElementDef> e = elementDefs;

        List<AtlasEnumDef.AtlasEnumElementDef> tmpList = new ArrayList<>();
        if (!e.isEmpty()) {
            // copy existing elements, except ones having same value as the element being added
            for (AtlasEnumDef.AtlasEnumElementDef existingElem : e) {
                if ((existingElem.getValue().equalsIgnoreCase(elementDef.getValue()))) {
                    tmpList.add(existingElem);
                }
            }
        }
        tmpList.add(new AtlasEnumDef.AtlasEnumElementDef(elementDef));

        elementDefs = tmpList;
    }

    public void removeElement(String elemValue) {
        List<AtlasEnumDef.AtlasEnumElementDef> e = elementDefs;

        // if element doesn't exist, no need to create the tmpList below
        if (hasElement(e, elemValue)) {
            List<AtlasEnumDef.AtlasEnumElementDef> tmpList = new ArrayList<>();

            // copy existing elements, except ones having same value as the element being removed
            for (AtlasEnumDef.AtlasEnumElementDef existingElem : e) {
                if (!existingElem.getValue().equalsIgnoreCase(elemValue)) {
                    tmpList.add(existingElem);
                }
            }

            elementDefs = tmpList;
        }
    }

    public boolean hasElement(String elemValue) {
        return getElement(elemValue) != null;
    }

    @Override
    public StringBuilder toString(StringBuilder sb) {
        if (sb == null) {
            sb = new StringBuilder();
        }

        sb.append("AtlasEnumDef{");
        super.toString(sb);
        sb.append(", elementDefs=[");
        dumpObjects(elementDefs, sb);
        sb.append("]");
        sb.append(", defaultValue {");
        sb.append(defaultValue);
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
        if (!super.equals(o)) {
            return false;
        }
        AtlasEnumDef that = (AtlasEnumDef) o;
        return Objects.equals(elementDefs, that.elementDefs) &&
                Objects.equals(defaultValue, that.defaultValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), elementDefs, defaultValue);
    }

    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }


    /**
     * class that captures details of an enum-element.
     */
    @JsonAutoDetect(getterVisibility = PUBLIC_ONLY, setterVisibility = PUBLIC_ONLY, fieldVisibility = NONE)
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AtlasEnumElementDef implements Serializable {
        private static final long serialVersionUID = 1L;

        private String value;
        private String description;
        private Integer ordinal;

        public AtlasEnumElementDef() {
            this(null, null, null);
        }

        public AtlasEnumElementDef(String value, String description, Integer ordinal) {
            setValue(value);
            setDescription(description);
            setOrdinal(ordinal);
        }

        public AtlasEnumElementDef(AtlasEnumDef.AtlasEnumElementDef other) {
            if (other != null) {
                setValue(other.getValue());
                setDescription(other.getDescription());
                setOrdinal(other.getOrdinal());
            }
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Integer getOrdinal() {
            return ordinal;
        }

        public void setOrdinal(Integer ordinal) {
            this.ordinal = ordinal;
        }

        public StringBuilder toString(StringBuilder sb) {
            if (sb == null) {
                sb = new StringBuilder();
            }

            sb.append("AtlasEnumElementDef{");
            sb.append("value='").append(value).append('\'');
            sb.append(", description='").append(description).append('\'');
            sb.append(", ordinal=").append(ordinal);
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
            AtlasEnumDef.AtlasEnumElementDef that = (AtlasEnumDef.AtlasEnumElementDef) o;
            return Objects.equals(value, that.value) &&
                    Objects.equals(description, that.description) &&
                    Objects.equals(ordinal, that.ordinal);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value, description, ordinal);
        }

        @Override
        public String toString() {
            return toString(new StringBuilder()).toString();
        }
    }

}
