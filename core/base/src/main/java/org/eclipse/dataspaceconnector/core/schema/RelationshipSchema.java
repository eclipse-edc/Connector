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

package org.eclipse.dataspaceconnector.core.schema;

import org.eclipse.dataspaceconnector.spi.types.domain.schema.Schema;

/**
 * Schema for relations between catalog entities, e.g. in Atlas
 */
public abstract class RelationshipSchema extends Schema {
    public abstract EndpointDefinition getStartDefinition();

    public abstract EndpointDefinition getEndDefinition();

    public String getDescription() {
        return null;
    }

    //0 ... association, 1... aggregation, 2... composition
    public int getRelationshipCategory() {
        return 1;
    }

    @Override
    protected void addAttributes() {
    }

    public static class EndpointDefinition {
        private final String typeName;
        private final String name;
        private final int cardinality; //0 ... single, 1... list, 2... set

        public EndpointDefinition(String typeName, String name) {
            this(typeName, name, 0);
        }

        public EndpointDefinition(String typeName, String name, int cardinality) {
            this.typeName = typeName;
            this.name = name;
            this.cardinality = cardinality;
        }

        public String getTypeName() {
            return typeName;
        }

        public String getName() {
            return name;
        }

        public int getCardinality() {
            return cardinality;
        }
    }
}
