/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.catalog.atlas.dto;


import java.util.*;

public class Functions {

    public static AtlasClassificationDef createTraitTypeDef(String typeName, Set<String> superTypes) {
        return new AtlasClassificationDef(typeName, null, "1.0", Collections.emptyList(), superTypes);
    }

    public static AtlasStructDef.AtlasAttributeDef createRequiredAttrDef(String name, String dataType) {
        return new AtlasStructDef.AtlasAttributeDef(name, dataType, false,
                AtlasStructDef.AtlasAttributeDef.Cardinality.SINGLE, 1, 1,
                false, true, false,
                Collections.<AtlasStructDef.AtlasConstraintDef>emptyList());
    }

    public static AtlasStructDef.AtlasAttributeDef createOptionalAttrDef(String name, String dataType) {
        return new AtlasStructDef.AtlasAttributeDef(name, dataType, true,
                AtlasStructDef.AtlasAttributeDef.Cardinality.SINGLE, 0, 1,
                false, false, false,
                Collections.<AtlasStructDef.AtlasConstraintDef>emptyList());
    }

    public static AtlasEntityDef createClassTypeDef(String name, Set<String> superTypes) {
        return new AtlasEntityDef(name, null, "1.0", Collections.emptyList(), superTypes);
    }

    public static AtlasRelationshipDef createRelationshipTypeDef(String name,
                                                                 String description,
                                                                 String version,
                                                                 AtlasRelationshipDef.RelationshipCategory relationshipCategory,
                                                                 AtlasRelationshipDef.PropagateTags propagateTags,
                                                                 AtlasRelationshipEndDef endDef1,
                                                                 AtlasRelationshipEndDef endDef2,
                                                                 AtlasStructDef.AtlasAttributeDef... attrDefs) {
        return new AtlasRelationshipDef(name, description, version, relationshipCategory, propagateTags,
                endDef1, endDef2, Arrays.asList(attrDefs));
    }

    public static <T> boolean isEmpty(Collection<T> collection) {
        return collection == null || collection.isEmpty();
    }

    public static <T> boolean isNotEmpty(Collection<T> collection) {
        return !isEmpty(collection);
    }

    public static <K, V> boolean isNotEmpty(Map<K, V> map) {
        return !isEmpty(map);
    }

    private static <V, K> boolean isEmpty(Map<K, V> map) {
        return map == null || map.isEmpty();
    }

    public static boolean equals(String str1, String str2) {
        return Objects.equals(str1, str2);
    }

    public static boolean equalsIgnoreCase(String str1, String str2) {
        return str1 == null ? str2 == null : str1.equalsIgnoreCase(str2);
    }
}
