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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;

@JsonAutoDetect(getterVisibility = PUBLIC_ONLY, setterVisibility = PUBLIC_ONLY, fieldVisibility = NONE)
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class EntityMutations implements Serializable {

    private List<EntityMutations.EntityMutation> entityMutations = new ArrayList<>();

    public EntityMutations(List<EntityMutations.EntityMutation> entityMutations) {
        this.entityMutations = entityMutations;
    }

    public StringBuilder toString(StringBuilder sb) {
        if (sb == null) {
            sb = new StringBuilder();
        }
        sb.append("EntityMutations{");
        if (!entityMutations.isEmpty()) {
            for (int i = 0; i < entityMutations.size(); i++) {
                if (i > 0) {
                    sb.append(",");
                }
                entityMutations.get(i).toString(sb);
            }
        }
        sb.append("}");

        return sb;
    }

    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EntityMutations that = (EntityMutations) o;
        return Objects.equals(entityMutations, that.entityMutations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entityMutations);
    }


    public enum EntityOperation {
        CREATE,
        UPDATE,
        PARTIAL_UPDATE,
        DELETE,
        PURGE
    }

    public static final class EntityMutation implements Serializable {
        private final EntityMutations.EntityOperation op;
        private final AtlasEntity entity;

        public EntityMutation(EntityMutations.EntityOperation op, AtlasEntity entity) {
            this.op = op;
            this.entity = entity;
        }

        public StringBuilder toString(StringBuilder sb) {
            if (sb == null) {
                sb = new StringBuilder();
            }
            sb.append("EntityMutation {");
            sb.append("op=").append(op);
            if (entity != null) {
                sb.append(", entity=");
                entity.toString(sb);
            }
            sb.append("}");

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
            EntityMutations.EntityMutation that = (EntityMutation) o;
            return op == that.op &&
                    Objects.equals(entity, that.entity);
        }

        @Override
        public int hashCode() {
            return Objects.hash(op, entity);
        }

        @Override
        public String toString() {
            return toString(new StringBuilder()).toString();
        }
    }
}