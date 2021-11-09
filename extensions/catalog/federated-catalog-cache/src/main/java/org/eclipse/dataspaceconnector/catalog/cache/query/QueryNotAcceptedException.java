/*
 * Copyright (c) 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *      Microsoft Corporation - initial API and implementation
 *
 *
 */

package org.eclipse.dataspaceconnector.catalog.cache.query;

import java.util.Collections;

public class QueryNotAcceptedException extends QueryException {
    public QueryNotAcceptedException() {
        super(Collections.singletonList("No suitable adapter found for the supplied query!"));
    }
}
