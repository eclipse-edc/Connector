/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.sql.transferprocess.store.schema.postgres;

import org.eclipse.dataspaceconnector.sql.translation.JsonFieldMapping;

/**
 * Maps fields of a {@link org.eclipse.dataspaceconnector.spi.types.domain.transfer.DeprovisionedResource} onto the
 * corresponding SQL schema (= column names) enabling access through Postgres JSON operators
 */
class DeprovisionedResourcesMapping extends JsonFieldMapping {

    DeprovisionedResourcesMapping(String columnName) {
        super(columnName);
    }
}
