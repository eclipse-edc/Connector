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

package org.eclipse.dataspaceconnector.spi.types.domain.schema;

import java.util.AbstractSet;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

public abstract class Schema {

    protected final AbstractSet<SchemaAttribute> attributes;

    protected Schema() {
        attributes = new LinkedHashSet<>();
        attributes.add(new SchemaAttribute("type", true));
        addAttributes();
    }

    protected abstract void addAttributes();

    /**
     * A string that uniquely identifies the schema. Can be scoped/namespaced
     */
    public abstract String getName();

    public AbstractSet<SchemaAttribute> getAttributes() {
        return attributes;
    }

    public AbstractSet<SchemaAttribute> getRequiredAttributes() {
        return getAttributes().stream().filter(SchemaAttribute::isRequired).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public String toString() {
        return getName();
    }
}
