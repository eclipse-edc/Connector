/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial Test
 *
 */

package org.eclipse.dataspaceconnector.sql.pool.commons;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CommonsConnectionPoolConfigTest {

    @Test
    void testDefaults() {
        CommonsConnectionPoolConfig commonsConnectionPoolConfig = CommonsConnectionPoolConfig.Builder.newInstance().build();

        Assertions.assertEquals(4, commonsConnectionPoolConfig.getMaxIdleConnections());
        Assertions.assertEquals(8, commonsConnectionPoolConfig.getMaxTotalConnections());
        Assertions.assertEquals(1, commonsConnectionPoolConfig.getMinIdleConnections());
        Assertions.assertTrue(commonsConnectionPoolConfig.getTestConnectionOnBorrow());
        Assertions.assertTrue(commonsConnectionPoolConfig.getTestConnectionOnCreate());
        Assertions.assertTrue(commonsConnectionPoolConfig.getTestConnectionOnReturn());
        Assertions.assertFalse(commonsConnectionPoolConfig.getTestConnectionWhileIdle());
        Assertions.assertEquals("SELECT 1;", commonsConnectionPoolConfig.getTestQuery());
    }

    @Test
    void test() {
        int minIdleConnections = 1;
        int maxIdleConnections = 2;
        int maxTotalConnections = 3;
        boolean testConnectionOnBorrow = true;
        boolean testConnectionOnCreate = false;
        boolean testConnectionWhileIdle = true;
        boolean testConnectionOnReturn = false;
        String testQuery = "testquery";

        CommonsConnectionPoolConfig commonsConnectionPoolConfig = CommonsConnectionPoolConfig.Builder.newInstance()
                .maxIdleConnections(maxIdleConnections)
                .maxTotalConnections(maxTotalConnections)
                .minIdleConnections(minIdleConnections)
                .testConnectionOnBorrow(testConnectionOnBorrow)
                .testConnectionOnCreate(testConnectionOnCreate)
                .testConnectionOnReturn(testConnectionOnReturn)
                .testConnectionWhileIdle(testConnectionWhileIdle)
                .testQuery(testQuery)
                .build();

        Assertions.assertEquals(maxIdleConnections, commonsConnectionPoolConfig.getMaxIdleConnections());
        Assertions.assertEquals(maxTotalConnections, commonsConnectionPoolConfig.getMaxTotalConnections());
        Assertions.assertEquals(minIdleConnections, commonsConnectionPoolConfig.getMinIdleConnections());
        Assertions.assertEquals(testConnectionOnBorrow, commonsConnectionPoolConfig.getTestConnectionOnBorrow());
        Assertions.assertEquals(testConnectionOnCreate, commonsConnectionPoolConfig.getTestConnectionOnCreate());
        Assertions.assertEquals(testConnectionOnReturn, commonsConnectionPoolConfig.getTestConnectionOnReturn());
        Assertions.assertEquals(testConnectionWhileIdle, commonsConnectionPoolConfig.getTestConnectionWhileIdle());
        Assertions.assertEquals(testQuery, commonsConnectionPoolConfig.getTestQuery());
    }
}
