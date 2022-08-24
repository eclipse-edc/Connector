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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderByClauseTest {

    @Test
    void asString() {
        assertThat(new OrderByClause("description", true, "c.wrappedObject").asString()).isEqualTo("ORDER BY c.wrappedObject.description ASC");
        assertThat(new OrderByClause("description", false, "c.wrappedObject").asString()).isEqualTo("ORDER BY c.wrappedObject.description DESC");
        assertThat(new OrderByClause("description", false, "c").asString()).isEqualTo("ORDER BY c.description DESC");
        assertThat(new OrderByClause(null, false, "c").asString()).isEmpty();
        assertThat(new OrderByClause("description", false, null).asString()).isEqualTo("ORDER BY description DESC");
    }

}