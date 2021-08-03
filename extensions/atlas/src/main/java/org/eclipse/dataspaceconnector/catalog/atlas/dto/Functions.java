/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package org.eclipse.dataspaceconnector.catalog.atlas.dto;


import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

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
        return new AtlasStructDef.AtlasAttributeDef(name, dataType, true,/**/
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


}
