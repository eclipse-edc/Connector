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

package org.eclipse.dataspaceconnector.core.schema.policy;

import org.eclipse.dataspaceconnector.spi.types.domain.schema.Schema;
import org.eclipse.dataspaceconnector.spi.types.domain.schema.SchemaAttribute;

public class PolicySchema extends Schema {
    public static String TYPE = "dataspaceconnector:policy";

    @Override
    protected void addAttributes() {
        attributes.add(new SchemaAttribute("namespace", false));
        attributes.add(new SchemaAttribute("serialized", true));
        attributes.add(new SchemaAttribute("validity-start", false));
        attributes.add(new SchemaAttribute("validity-end", false));
        attributes.add(new SchemaAttribute("policyName", false));
    }

    @Override
    public String getName() {
        return TYPE;
    }
}
