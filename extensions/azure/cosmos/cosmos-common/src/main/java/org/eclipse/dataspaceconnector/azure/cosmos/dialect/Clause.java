/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.azure.cosmos.dialect;

import com.azure.cosmos.models.SqlParameter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Interface for all SQL-like statements or parts thereof. Offers conversion to String plus an optional list of parameters.
 */
interface Clause {
    String asString();

    @NotNull
    default List<SqlParameter> getParameters() {
        return List.of();
    }
}
