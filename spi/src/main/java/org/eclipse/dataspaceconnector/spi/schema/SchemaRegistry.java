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

package org.eclipse.dataspaceconnector.spi.schema;

import org.eclipse.dataspaceconnector.spi.system.Feature;
import org.eclipse.dataspaceconnector.spi.types.domain.schema.Schema;

import java.util.Collection;

@Feature(SchemaRegistry.FEATURE)
public interface SchemaRegistry {
    String FEATURE = "edc:core:base:schema-registry";

    void register(Schema schema);

    Schema getSchema(String identifier);

    boolean hasSchema(String identifier);

    Collection<Schema> getSchemas();
}
