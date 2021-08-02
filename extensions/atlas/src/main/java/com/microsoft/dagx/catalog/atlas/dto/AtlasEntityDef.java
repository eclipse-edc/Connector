/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.catalog.atlas.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.microsoft.dagx.common.collection.CollectionUtil;

import java.io.Serializable;
import java.util.*;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;

@JsonAutoDetect(getterVisibility = PUBLIC_ONLY, setterVisibility = PUBLIC_ONLY, fieldVisibility = NONE)
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AtlasEntityDef extends AtlasStructDef implements java.io.Serializable {
    public static final String OPTION_DISPLAY_TEXT_ATTRIBUTE = "displayTextAttribute";
    private static final long serialVersionUID = 1L;
    private Set<String> superTypes;

    // this is a read-only field, any value provided during create & update operation is ignored
    // the value of this field is derived from 'superTypes' specified in all AtlasEntityDef
    private Set<String> subTypes;

    // this is a read-only field, any value provided during create & update operation is ignored
    // the value of this field is derived from all the relationshipDefs this entityType is referenced in
    private List<AtlasEntityDef.AtlasRelationshipAttributeDef> relationshipAttributeDefs;

    // this is a read-only field, any value provided during create & update operation is ignored
    // the value of this field is derived from all the businessMetadataDefs this entityType is referenced in
    private Map<String, List<AtlasAttributeDef>> businessAttributeDefs;


    public AtlasEntityDef() {
        this(null, null, null, null, null, null, null);
    }

    public AtlasEntityDef(String name) {
        this(name, null, null, null, null, null, null);
    }

    public AtlasEntityDef(String name, String description) {
        this(name, description, null, null, null, null, null);
    }

    public AtlasEntityDef(String name, String description, String typeVersion) {
        this(name, description, typeVersion, null, null, null, null);
    }

    public AtlasEntityDef(String name, String description, String typeVersion, String serviceType) {
        this(name, description, typeVersion, serviceType, null, null, null);
    }


    public AtlasEntityDef(String name, String description, String typeVersion, List<AtlasAttributeDef> attributeDefs) {
        this(name, description, typeVersion, attributeDefs, null);
    }

    public AtlasEntityDef(String name, String description, String typeVersion, String serviceType, List<AtlasAttributeDef> attributeDefs) {
        this(name, description, typeVersion, serviceType, attributeDefs, null, null);
    }

    public AtlasEntityDef(String name, String description, String typeVersion, List<AtlasAttributeDef> attributeDefs,
                          Set<String> superTypes) {
        this(name, description, typeVersion, attributeDefs, superTypes, null);
    }

    public AtlasEntityDef(String name, String description, String typeVersion, String serviceType, List<AtlasAttributeDef> attributeDefs,
                          Set<String> superTypes) {
        this(name, description, typeVersion, serviceType, attributeDefs, superTypes, null);
    }


    public AtlasEntityDef(String name, String description, String typeVersion, List<AtlasAttributeDef> attributeDefs,
                          Set<String> superTypes, Map<String, String> options) {
        super(TypeCategory.ENTITY, name, description, typeVersion, attributeDefs, options);

        setSuperTypes(superTypes);
    }

    public AtlasEntityDef(String name, String description, String typeVersion, String serviceType, List<AtlasAttributeDef> attributeDefs,
                          Set<String> superTypes, Map<String, String> options) {
        super(TypeCategory.ENTITY, name, description, typeVersion, attributeDefs, serviceType, options);

        setSuperTypes(superTypes);
    }


    public AtlasEntityDef(AtlasEntityDef other) {
        super(other);

        if (other != null) {
            setSuperTypes(other.getSuperTypes());
            setSubTypes(other.getSubTypes());
            setRelationshipAttributeDefs(other.getRelationshipAttributeDefs());
            setBusinessAttributeDefs(other.getBusinessAttributeDefs());
        }
    }

    private static boolean hasSuperType(Set<String> superTypes, String typeName) {
        return superTypes != null && typeName != null && superTypes.contains(typeName);
    }

    public Set<String> getSuperTypes() {
        return superTypes;
    }

    public void setSuperTypes(Set<String> superTypes) {
        if (superTypes != null && this.superTypes == superTypes) {
            return;
        }

        if (CollectionUtil.isEmpty(superTypes)) {
            this.superTypes = new HashSet<>();
        } else {
            this.superTypes = new HashSet<>(superTypes);
        }
    }

    public Set<String> getSubTypes() {
        return subTypes;
    }

    public void setSubTypes(Set<String> subTypes) {
        this.subTypes = subTypes;
    }

    public List<AtlasEntityDef.AtlasRelationshipAttributeDef> getRelationshipAttributeDefs() {
        return relationshipAttributeDefs;
    }

    public void setRelationshipAttributeDefs(List<AtlasEntityDef.AtlasRelationshipAttributeDef> relationshipAttributeDefs) {
        this.relationshipAttributeDefs = relationshipAttributeDefs;
    }

    public Map<String, List<AtlasAttributeDef>> getBusinessAttributeDefs() {
        return businessAttributeDefs;
    }

    public void setBusinessAttributeDefs(Map<String, List<AtlasAttributeDef>> businessAttributeDefs) {
        this.businessAttributeDefs = businessAttributeDefs;
    }

    public boolean hasSuperType(String typeName) {
        return hasSuperType(superTypes, typeName);
    }

    public void addSuperType(String typeName) {
        Set<String> s = superTypes;

        if (!hasSuperType(s, typeName)) {
            s = new HashSet<>(s);

            s.add(typeName);

            superTypes = s;
        }
    }

    public void removeSuperType(String typeName) {
        Set<String> s = superTypes;

        if (hasSuperType(s, typeName)) {
            s = new HashSet<>(s);

            s.remove(typeName);

            superTypes = s;
        }
    }

    @Override
    public StringBuilder toString(StringBuilder sb) {
        if (sb == null) {
            sb = new StringBuilder();
        }

        sb.append("AtlasEntityDef{");
        super.toString(sb);
        sb.append(", superTypes=[");
        dumpObjects(superTypes, sb);
        sb.append("]");
        sb.append(", relationshipAttributeDefs=[");
        if (CollectionUtil.isNotEmpty(relationshipAttributeDefs)) {
            int i = 0;
            for (AtlasEntityDef.AtlasRelationshipAttributeDef attributeDef : relationshipAttributeDefs) {
                if (i > 0) {
                    sb.append(", ");
                }

                attributeDef.toString(sb);

                i++;
            }
        }
        sb.append(']');
        sb.append(", businessAttributeDefs={");
        if (CollectionUtil.isNotEmpty(businessAttributeDefs)) {
            int nsIdx = 0;

            for (Map.Entry<String, List<AtlasAttributeDef>> entry : businessAttributeDefs.entrySet()) {
                String nsName = entry.getKey();
                List<AtlasAttributeDef> nsAttrs = entry.getValue();

                if (nsIdx > 0) {
                    sb.append(", ");
                }

                sb.append(nsName).append("=[");

                int attrIdx = 0;
                for (AtlasAttributeDef attributeDef : nsAttrs) {
                    if (attrIdx > 0) {
                        sb.append(", ");
                    }

                    attributeDef.toString(sb);

                    attrIdx++;
                }
                sb.append(']');

                nsIdx++;
            }
        }
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

        AtlasEntityDef that = (AtlasEntityDef) o;
        return Objects.equals(superTypes, that.superTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), superTypes);
    }

    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }

    /**
     * class that captures details of a struct-attribute.
     */
    @JsonAutoDetect(getterVisibility = PUBLIC_ONLY, setterVisibility = PUBLIC_ONLY, fieldVisibility = NONE)
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AtlasRelationshipAttributeDef extends AtlasAttributeDef implements Serializable {
        private static final long serialVersionUID = 1L;

        private String relationshipTypeName;
        private boolean isLegacyAttribute;

        public AtlasRelationshipAttributeDef() {
        }

        public AtlasRelationshipAttributeDef(String relationshipTypeName, boolean isLegacyAttribute, AtlasAttributeDef attributeDef) {
            super(attributeDef);

            this.relationshipTypeName = relationshipTypeName;
            this.isLegacyAttribute = isLegacyAttribute;
        }

        public String getRelationshipTypeName() {
            return relationshipTypeName;
        }

        public void setRelationshipTypeName(String relationshipTypeName) {
            this.relationshipTypeName = relationshipTypeName;
        }

        public boolean getIsLegacyAttribute() {
            return isLegacyAttribute;
        }

        public void setIsLegacyAttribute(boolean isLegacyAttribute) {
            this.isLegacyAttribute = isLegacyAttribute;
        }

        @Override
        public StringBuilder toString(StringBuilder sb) {
            if (sb == null) {
                sb = new StringBuilder();
            }

            sb.append("AtlasRelationshipAttributeDef{");
            super.toString(sb);
            sb.append(", relationshipTypeName='").append(relationshipTypeName).append('\'');
            sb.append(", isLegacyAttribute='").append(isLegacyAttribute).append('\'');
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

            AtlasEntityDef.AtlasRelationshipAttributeDef that = (AtlasEntityDef.AtlasRelationshipAttributeDef) o;

            return super.equals(that) &&
                    isLegacyAttribute == that.isLegacyAttribute &&
                    Objects.equals(relationshipTypeName, that.relationshipTypeName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), relationshipTypeName, isLegacyAttribute);
        }

        @Override
        public String toString() {
            return toString(new StringBuilder()).toString();
        }
    }
}
