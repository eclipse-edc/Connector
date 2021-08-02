/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.catalog.atlas.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.*;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;

@JsonAutoDetect(getterVisibility = PUBLIC_ONLY, setterVisibility = PUBLIC_ONLY, fieldVisibility = NONE)
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AtlasClassificationDef extends AtlasStructDef implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private Set<String> superTypes;
    private Set<String> entityTypes;

    // subTypes field below is derived from 'superTypes' specified in all AtlasClassificationDef
    // this value is ignored during create & update operations
    private Set<String> subTypes;

    public AtlasClassificationDef() {
        this(null, null, null, null, null, null);
    }


    public AtlasClassificationDef(String name, String description, String typeVersion,
                                  List<AtlasAttributeDef> attributeDefs, Set<String> superTypes) {
        this(name, description, typeVersion, attributeDefs, superTypes, null);
    }

    public AtlasClassificationDef(String name, String description, String typeVersion,
                                  List<AtlasAttributeDef> attributeDefs, Set<String> superTypes,
                                  Map<String, String> options) {
        this(name, description, typeVersion, attributeDefs, superTypes, null, options);
    }

    public AtlasClassificationDef(String name, String description, String typeVersion,
                                  List<AtlasAttributeDef> attributeDefs, Set<String> superTypes,
                                  Set<String> entityTypes, Map<String, String> options) {
        super(TypeCategory.CLASSIFICATION, name, description, typeVersion, attributeDefs, options);

        setSuperTypes(superTypes);
        setEntityTypes(entityTypes);
    }

    private static boolean hasSuperType(Set<String> superTypes, String typeName) {
        return superTypes != null && typeName != null && superTypes.contains(typeName);
    }

    private static boolean hasEntityType(Set<String> entityTypes, String typeName) {
        return entityTypes != null && typeName != null && entityTypes.contains(typeName);
    }

    public Set<String> getSuperTypes() {
        return superTypes;
    }

    public void setSuperTypes(Set<String> superTypes) {
        if (superTypes != null && this.superTypes == superTypes) {
            return;
        }

        if (superTypes == null || superTypes.isEmpty()) {
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

    /**
     * Specifying a list of entityType names in the classificationDef, ensures that classifications can
     * only be applied to those entityTypes.
     * <ul>
     * <li>Any subtypes of the entity types inherit the restriction</li>
     * <li>Any classificationDef subtypes inherit the parents entityTypes restrictions</li>
     * <li>Any classificationDef subtypes can further restrict the parents entityTypes restrictions by specifying a subset of the entityTypes</li>
     * <li>An empty entityTypes list when there are no parent restrictions means there are no restrictions</li>
     * <li>An empty entityTypes list when there are parent restrictions means that the subtype picks up the parents restrictions</li>
     * <li>If a list of entityTypes are supplied, where one inherits from another, this will be rejected. This should encourage cleaner classificationsDefs</li>
     * </ul>
     */
    public Set<String> getEntityTypes() {
        return entityTypes;
    }

    public void setEntityTypes(Set<String> entityTypes) {
        if (entityTypes != null && this.entityTypes == entityTypes) {
            return;
        }

        if (entityTypes == null || entityTypes.isEmpty()) {
            this.entityTypes = new HashSet<>();
        } else {
            this.entityTypes = new HashSet<>(entityTypes);
        }
    }

    public boolean hasEntityType(String typeName) {
        return hasEntityType(entityTypes, typeName);
    }

    public void addEntityType(String typeName) {
        Set<String> s = entityTypes;

        if (!hasEntityType(s, typeName)) {
            s = new HashSet<>(s);

            s.add(typeName);

            entityTypes = s;
        }
    }

    public void removeEntityType(String typeName) {
        Set<String> s = entityTypes;

        if (hasEntityType(s, typeName)) {
            s = new HashSet<>(s);

            s.remove(typeName);

            entityTypes = s;
        }
    }

    @Override
    public StringBuilder toString(StringBuilder sb) {
        if (sb == null) {
            sb = new StringBuilder();
        }

        sb.append("AtlasClassificationDef{");
        super.toString(sb);
        sb.append(", superTypes=[");
        dumpObjects(superTypes, sb);
        sb.append("], entityTypes=[");
        dumpObjects(entityTypes, sb);
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
        if (!super.equals(o)) {
            return false;
        }

        AtlasClassificationDef that = (AtlasClassificationDef) o;

        return Objects.equals(superTypes, that.superTypes) && Objects.equals(entityTypes, that.entityTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), superTypes);
    }

    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }

}