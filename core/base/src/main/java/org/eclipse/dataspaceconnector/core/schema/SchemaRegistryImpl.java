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

import org.eclipse.dataspaceconnector.spi.schema.SchemaRegistry;
import org.eclipse.dataspaceconnector.spi.types.domain.schema.Schema;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SchemaRegistryImpl implements SchemaRegistry {

    private final Map<String, Schema> schemas;

    public SchemaRegistryImpl() {
        schemas = new HashMap<>();
    }

    @Override
    public void register(Schema schema) {
        schemas.put(schema.getName(), schema);
    }

    @Override
    public Schema getSchema(String identifier) {
        return schemas.get(identifier);
    }

    @Override
    public boolean hasSchema(String identifier) {
        return schemas.containsKey(identifier);
    }

    @Override
    public Collection<Schema> getSchemas() {
        return schemas.values();
    }
}
