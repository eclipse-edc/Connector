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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;

@JsonAutoDetect(getterVisibility = PUBLIC_ONLY, setterVisibility = PUBLIC_ONLY, fieldVisibility = NONE)
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AtlasClassification extends AtlasStruct implements Serializable {
    private static final long serialVersionUID = 1L;

    private String entityGuid = null;
    private AtlasEntity.Status entityStatus = AtlasEntity.Status.ACTIVE;
    private Boolean propagate = null;
    private List<TimeBoundary> validityPeriods = null;
    private Boolean removePropagationsOnEntityDelete = null;

    public AtlasClassification() {
        this(null, null);
    }

    public AtlasClassification(String typeName) {
        this(typeName, null);
    }

    public AtlasClassification(String typeName, Map<String, Object> attributes) {
        super(typeName, attributes);
    }

    public AtlasClassification(String typeName, String attrName, Object attrValue) {
        super(typeName, attrName, attrValue);
    }

    public AtlasClassification(Map map) {
        super(map);
    }

    public AtlasClassification(AtlasClassification other) {
        if (other != null) {
            setTypeName(other.getTypeName());
            setAttributes(other.getAttributes());
            setEntityGuid(other.getEntityGuid());
            setEntityStatus(other.getEntityStatus());
            setPropagate(other.isPropagate());
            setValidityPeriods(other.getValidityPeriods());
            setRemovePropagationsOnEntityDelete(other.getRemovePropagationsOnEntityDelete());
        }
    }

    public String getEntityGuid() {
        return entityGuid;
    }

    public void setEntityGuid(String entityGuid) {
        this.entityGuid = entityGuid;
    }

    public Boolean isPropagate() {
        return propagate;
    }

    public Boolean getPropagate() {
        return propagate;
    }

    public void setPropagate(Boolean propagate) {
        this.propagate = propagate;
    }

    public List<TimeBoundary> getValidityPeriods() {
        return validityPeriods;
    }

    public void setValidityPeriods(List<TimeBoundary> validityPeriods) {
        this.validityPeriods = validityPeriods;
    }

    public AtlasEntity.Status getEntityStatus() {
        return entityStatus;
    }

    public void setEntityStatus(AtlasEntity.Status entityStatus) {
        this.entityStatus = entityStatus;
    }

    public Boolean getRemovePropagationsOnEntityDelete() {
        return removePropagationsOnEntityDelete;
    }

    public void setRemovePropagationsOnEntityDelete(Boolean removePropagationsOnEntityDelete) {
        this.removePropagationsOnEntityDelete = removePropagationsOnEntityDelete;
    }

    @JsonIgnore
    public void addValityPeriod(TimeBoundary validityPeriod) {
        List<TimeBoundary> vpList = validityPeriods;

        if (vpList == null) {
            vpList = new ArrayList<>();

            validityPeriods = vpList;
        }

        vpList.add(validityPeriod);
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
        AtlasClassification that = (AtlasClassification) o;
        return Objects.equals(propagate, that.propagate) &&
                Objects.equals(removePropagationsOnEntityDelete, that.removePropagationsOnEntityDelete) &&
                Objects.equals(entityGuid, that.entityGuid) &&
                entityStatus == that.entityStatus &&
                Objects.equals(validityPeriods, that.validityPeriods);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), entityGuid, entityStatus, propagate, removePropagationsOnEntityDelete);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("AtlasClassification{");
        super.toString(sb);
        sb.append("entityGuid='").append(entityGuid).append('\'');
        sb.append(", entityStatus=").append(entityStatus);
        sb.append(", propagate=").append(propagate);
        sb.append(", removePropagationsOnEntityDelete=").append(removePropagationsOnEntityDelete);
        sb.append(", validityPeriods=").append(validityPeriods);
        sb.append(", validityPeriods=").append(validityPeriods);
        sb.append('}');
        return sb.toString();
    }

}