/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.catalog.atlas.metadata;

import org.eclipse.dataspaceconnector.catalog.atlas.dto.AtlasEntity;
import org.eclipse.dataspaceconnector.catalog.atlas.dto.AtlasSearchResult;
import org.eclipse.dataspaceconnector.catalog.atlas.dto.AtlasTypesDef;
import org.eclipse.dataspaceconnector.schema.RelationshipSchema;
import org.eclipse.dataspaceconnector.schema.SchemaAttribute;
import org.eclipse.dataspaceconnector.spi.EdcException;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface AtlasApi {
    /**
     * Creates a classification that can then be used to classify entities.
     *
     * @param classificationName The name of the classification
     * @return the resulting type definition
     * @throws EdcException if a classification with the same name already exists
     */
    AtlasTypesDef createClassifications(String... classificationName);

    /**
     * Deletes a classification
     *
     * @param classificationName The name of the classification to delete
     * @throws EdcException if a classification with that name does not exist
     */
    void deleteClassification(String classificationName);

    /**
     * Creates a custom entity type. This needs to be done before any entity of that type can be created. In OO terms this could
     * be thought of as defining a class.
     * If an entity type with that same name already exists, it will perform an update. Underneath, Atlas works with mutations, therefore
     * resulting in either a CREATE or an UPDATE mutation.
     *
     * @param typeName       The name of the type.
     * @param superTypeNames A set of super type names, or an empty set if none is desired.
     * @param attributes     A list of attributes (i.e. "class members") that that type should have. Note that attributes are inherited from supertypes,
     *                       especially required ones.
     * @return an AtlasTypeDef that contains the new type definition.
     * @throws EdcException if the custom type has an invalid attribute (e.g. invalid type), or if an invalid attempt was made to
     *                      alter a required attribute, or if a required attribute was added.
     */
    AtlasTypesDef createCustomTypes(String typeName, Set<String> superTypeNames, List<? extends SchemaAttribute> attributes);

    /**
     * Deletes a custom type
     *
     * @param typeName The type name
     * @throws EdcException if a type with that name does not exist.
     */
    void deleteCustomType(String typeName);

    /**
     * Creates a relationship between two entities. Note that for that to work, both the source and target entities and the relationship type must exist.
     *
     * @param sourceEntityGuid The ID (GUID) of the source entity.
     * @param targetEntityGuid The ID (GUID) of the target entity.
     * @param name             The relationship type name
     * @return An object containing information about that relation.
     * @throws EdcException if either the source or target entities do not exist, or a relationship type with the given name does not exist,
     *                      or that particular relationship already exists
     */
    AtlasRelationship createRelationship(String sourceEntityGuid, String targetEntityGuid, String name);

    /**
     * Deletes an {@code AtlasTypesDef} object. This is equivalent to a batch operation, that deletes all the referenced Atlas types, e.g. {@code AtlasTypesDef#getClassificationDefs()}
     *
     * @param type A wrapper object around atlas type definitions.
     */
    void deleteType(List<AtlasTypesDef> type);

    /**
     * Searches an Atlas entity by ID (GUID)
     *
     * @param id a valid GUID
     * @return an Atlas entity if found, null otherwise.
     */
    AtlasEntity.AtlasEntityWithExtInfo getEntityById(String id);

    /**
     * Creates an atlas entity, i.e. an instance of a (custom) type. If that entity already exists, e.g. DataSets and their subtypes are identified by the {@code qualifiedName}
     * property, then the entity will get updated.
     *
     * @param entityTypeName The name of the type (cf. class name).
     * @param properties     A list of properties that this entity should have. Note that this must satisfy all the required attributes that were specified when creating the type,
     *                       as well as all the super types' required attributes. It can not contain attributes that were not specified beforehand, so the schema is strict.
     * @return the newly created entity's ID (GUID)
     * @throws EdcException if a rogue attribute was specified, not all required attributes were specified, or if an attribute type does not match.
     */
    String createEntity(String entityTypeName, Map<String, Object> properties);

    /**
     * Batch-deletes a set of entities. This only moves them to the DELETED state, and they need to be purged to "really" remove them from Atlas. If any of the GUIDs does not
     * exist, no exception is thrown.
     *
     * @param entityGuids A list of GUIDs of to-delete entities.
     */
    void deleteEntities(List<String> entityGuids);

    /**
     * Creates a relationship type. Similar to custom type definitions, relationships require that a their type is defined in advance.
     *
     * @param name                 The name of the relationship type, cf. "class name"
     * @param description          An optional description
     * @param relationshipCategory The type of relation:  0 ... association, 1... aggregation, 2... composition
     * @param startDefinition      An abstract definition of the start point, i.e. from which entity type the relationship originates
     * @param endDefinition        An abstract definition of the end point, i.e. to which entity type the relationship leads
     * @return an AtlasTypesDef
     * @throws EdcException if that relationship type already exists, if the category is invalid or any of the start- or endpoint definition types don't exist
     */
    AtlasTypesDef createRelationshipType(String name, String description, int relationshipCategory, RelationshipSchema.EndpointDefinition startDefinition, RelationshipSchema.EndpointDefinition endDefinition);

    /**
     * Searches for all types (entities, enums, relationships) with a particular name. Wildcards are not supported, for that please use {@code dslSearchWithParams}
     *
     * @param name the type name.
     */
    AtlasTypesDef getAllTypes(String name);

    /**
     * Performs a query according to Atlas' own query DSL (https://atlas.apache.org/1.2.0/Search-Advanced.html).
     *
     * @param query  The query expression.
     * @param limit  maximum amount of results
     * @param offset page offset, how many results are to be skipped.
     */
    AtlasSearchResult dslSearchWithParams(String query, int limit, int offset);
}
