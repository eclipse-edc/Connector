/*
 * Copyright (c) Microsoft Corporation.
 *  All rights reserved.
 *
 */

package com.microsoft.dagx.catalog.atlas.dto;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.*;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;

@JsonAutoDetect(getterVisibility = PUBLIC_ONLY, setterVisibility = PUBLIC_ONLY, fieldVisibility = NONE)
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class EntityMutationResponse {

    private Map<EntityMutations.EntityOperation, List<AtlasEntityHeader>> mutatedEntities;
    private Map<String, String> guidAssignments;

    public EntityMutationResponse() {
    }

    public EntityMutationResponse(final Map<EntityMutations.EntityOperation, List<AtlasEntityHeader>> mutatedEntities) {
        this.mutatedEntities = mutatedEntities;
    }

    public Map<EntityMutations.EntityOperation, List<AtlasEntityHeader>> getMutatedEntities() {
        return mutatedEntities;
    }

    public void setMutatedEntities(final Map<EntityMutations.EntityOperation, List<AtlasEntityHeader>> mutatedEntities) {
        this.mutatedEntities = mutatedEntities;
    }

    public Map<String, String> getGuidAssignments() {
        return guidAssignments;
    }

    public void setGuidAssignments(Map<String, String> guidAssignments) {
        this.guidAssignments = guidAssignments;
    }

    @JsonIgnore
    public List<AtlasEntityHeader> getEntitiesByOperation(EntityMutations.EntityOperation op) {
        if (mutatedEntities != null) {
            return mutatedEntities.get(op);
        }
        return null;
    }

    @JsonIgnore
    public List<AtlasEntityHeader> getCreatedEntities() {
        if (mutatedEntities != null) {
            return mutatedEntities.get(EntityMutations.EntityOperation.CREATE);
        }
        return null;
    }

    @JsonIgnore
    public List<AtlasEntityHeader> getUpdatedEntities() {
        if (mutatedEntities != null) {
            return mutatedEntities.get(EntityMutations.EntityOperation.UPDATE);
        }
        return null;
    }

    public List<AtlasEntityHeader> getPartialUpdatedEntities() {
        if (mutatedEntities != null) {
            return mutatedEntities.get(EntityMutations.EntityOperation.PARTIAL_UPDATE);
        }
        return null;
    }

    @JsonIgnore
    public List<AtlasEntityHeader> getDeletedEntities() {
        if (mutatedEntities != null) {
            return mutatedEntities.get(EntityMutations.EntityOperation.DELETE);
        }
        return null;
    }

    @JsonIgnore
    public List<AtlasEntityHeader> getPurgedEntities() {
        if (mutatedEntities != null) {
            return mutatedEntities.get(EntityMutations.EntityOperation.PURGE);
        }
        return null;
    }

    @JsonIgnore
    public String getPurgedEntitiesIds() {
        String ret = null;
        List<AtlasEntityHeader> purgedEntities = getPurgedEntities();

        if (!purgedEntities.isEmpty()) {
            List<String> entityIds = purgedEntities.stream().map(entity -> entity.getGuid()).collect(Collectors.toList());

            ret = String.join(",", entityIds);
        }

        return ret;
    }

    @JsonIgnore
    public AtlasEntityHeader getFirstEntityCreated() {
        final List<AtlasEntityHeader> entitiesByOperation = getEntitiesByOperation(EntityMutations.EntityOperation.CREATE);
        if (entitiesByOperation != null && entitiesByOperation.size() > 0) {
            return entitiesByOperation.get(0);
        }

        return null;
    }

    @JsonIgnore
    public AtlasEntityHeader getFirstEntityUpdated() {
        final List<AtlasEntityHeader> entitiesByOperation = getEntitiesByOperation(EntityMutations.EntityOperation.UPDATE);

        if (entitiesByOperation != null && entitiesByOperation.size() > 0) {
            return entitiesByOperation.get(0);
        }

        return null;
    }

    @JsonIgnore
    public AtlasEntityHeader getFirstEntityPartialUpdated() {
        final List<AtlasEntityHeader> entitiesByOperation = getEntitiesByOperation(EntityMutations.EntityOperation.PARTIAL_UPDATE);
        if (entitiesByOperation != null && entitiesByOperation.size() > 0) {
            return entitiesByOperation.get(0);
        }

        return null;
    }

    @JsonIgnore
    public AtlasEntityHeader getFirstCreatedEntityByTypeName(String typeName) {
        return getFirstEntityByType(getEntitiesByOperation(EntityMutations.EntityOperation.CREATE), typeName);
    }

    @JsonIgnore
    public AtlasEntityHeader getFirstDeletedEntityByTypeName(String typeName) {
        return getFirstEntityByType(getEntitiesByOperation(EntityMutations.EntityOperation.DELETE), typeName);
    }

    @JsonIgnore
    public List<AtlasEntityHeader> getCreatedEntitiesByTypeName(String typeName) {
        return getEntitiesByType(getEntitiesByOperation(EntityMutations.EntityOperation.CREATE), typeName);
    }

    @JsonIgnore
    public List<AtlasEntityHeader> getPartialUpdatedEntitiesByTypeName(String typeName) {
        return getEntitiesByType(getEntitiesByOperation(EntityMutations.EntityOperation.PARTIAL_UPDATE), typeName);
    }

    @JsonIgnore
    public AtlasEntityHeader getCreatedEntityByTypeNameAndAttribute(String typeName, String attrName, String attrVal) {
        return getEntityByTypeAndUniqueAttribute(getEntitiesByOperation(EntityMutations.EntityOperation.CREATE), typeName, attrName, attrVal);
    }

    @JsonIgnore

    public AtlasEntityHeader getUpdatedEntityByTypeNameAndAttribute(String typeName, String attrName, String attrVal) {
        return getEntityByTypeAndUniqueAttribute(getEntitiesByOperation(EntityMutations.EntityOperation.UPDATE), typeName, attrName, attrVal);
    }

    @JsonIgnore
    public List<AtlasEntityHeader> getUpdatedEntitiesByTypeName(String typeName) {
        return getEntitiesByType(getEntitiesByOperation(EntityMutations.EntityOperation.UPDATE), typeName);
    }

    @JsonIgnore
    public List<AtlasEntityHeader> getDeletedEntitiesByTypeName(String typeName) {
        return getEntitiesByType(getEntitiesByOperation(EntityMutations.EntityOperation.DELETE), typeName);
    }

    @JsonIgnore
    public AtlasEntityHeader getFirstUpdatedEntityByTypeName(String typeName) {
        return getFirstEntityByType(getEntitiesByOperation(EntityMutations.EntityOperation.UPDATE), typeName);
    }

    @JsonIgnore
    public AtlasEntityHeader getFirstPartialUpdatedEntityByTypeName(String typeName) {
        return getFirstEntityByType(getEntitiesByOperation(EntityMutations.EntityOperation.PARTIAL_UPDATE), typeName);
    }

    @JsonIgnore
    public void addEntity(EntityMutations.EntityOperation op, AtlasEntityHeader header) {
        // if an entity is already included in CREATE, update the header, to capture propagated classifications
        if (op == EntityMutations.EntityOperation.UPDATE || op == EntityMutations.EntityOperation.PARTIAL_UPDATE) {
            if (entityHeaderExists(getCreatedEntities(), header.getGuid())) {
                op = EntityMutations.EntityOperation.CREATE;
            }
        }

        if (mutatedEntities == null) {
            mutatedEntities = new HashMap<>();
        }

        List<AtlasEntityHeader> opEntities = mutatedEntities.get(op);

        if (opEntities == null) {
            opEntities = new ArrayList<>();
            mutatedEntities.put(op, opEntities);
        }

        if (!entityHeaderExists(opEntities, header.getGuid())) {
            opEntities.add(header);
        }
    }

    private boolean entityHeaderExists(List<AtlasEntityHeader> entityHeaders, String guid) {
        boolean ret = false;

        if (!entityHeaders.isEmpty() && guid != null) {
            for (AtlasEntityHeader entityHeader : entityHeaders) {
                if (guid.equals(entityHeader.getGuid())) {
                    ret = true;
                    break;
                }
            }
        }

        return ret;
    }

    public StringBuilder toString(StringBuilder sb) {
        if (sb == null) {
            sb = new StringBuilder();
        }

        AtlasBaseTypeDef.dumpObjects(mutatedEntities, sb);

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
        EntityMutationResponse that = (EntityMutationResponse) o;
        return Objects.equals(mutatedEntities, that.mutatedEntities) &&
                Objects.equals(guidAssignments, that.guidAssignments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mutatedEntities, guidAssignments);
    }

    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }

    private AtlasEntityHeader getFirstEntityByType(List<AtlasEntityHeader> entitiesByOperation, String typeName) {
        if (entitiesByOperation != null && entitiesByOperation.size() > 0) {
            for (AtlasEntityHeader header : entitiesByOperation) {
                if (header.getTypeName().equals(typeName)) {
                    return header;
                }
            }
        }
        return null;
    }

    private List<AtlasEntityHeader> getEntitiesByType(List<AtlasEntityHeader> entitiesByOperation, String typeName) {
        List<AtlasEntityHeader> ret = new ArrayList<>();

        if (entitiesByOperation != null && entitiesByOperation.size() > 0) {
            for (AtlasEntityHeader header : entitiesByOperation) {
                if (header.getTypeName().equals(typeName)) {
                    ret.add(header);
                }
            }
        }
        return ret;
    }

    private AtlasEntityHeader getEntityByTypeAndUniqueAttribute(List<AtlasEntityHeader> entitiesByOperation, String typeName, String attrName, String attrVal) {
        if (entitiesByOperation != null && entitiesByOperation.size() > 0) {
            for (AtlasEntityHeader header : entitiesByOperation) {
                if (header.getTypeName().equals(typeName)) {
                    if (attrVal != null && attrVal.equals(header.getAttribute(attrName))) {
                        return header;
                    }
                }
            }
        }
        return null;
    }
}
