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

package org.eclipse.edc.sql.pool.commons;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommonsConnectionPoolConfigTest {

    @Test
    void testDefaults() {
        var commonsConnectionPoolConfig = CommonsConnectionPoolConfig.Builder.newInstance().build();

        assertEquals(4, commonsConnectionPoolConfig.getMaxIdleConnections());
        assertEquals(8, commonsConnectionPoolConfig.getMaxTotalConnections());
        assertEquals(1, commonsConnectionPoolConfig.getMinIdleConnections());
        assertTrue(commonsConnectionPoolConfig.getTestConnectionOnBorrow());
        assertTrue(commonsConnectionPoolConfig.getTestConnectionOnCreate());
        assertFalse(commonsConnectionPoolConfig.getTestConnectionOnReturn());
        assertFalse(commonsConnectionPoolConfig.getTestConnectionWhileIdle());
        assertEquals("SELECT 1;", commonsConnectionPoolConfig.getTestQuery());
    }

    @Test
    void test() {
        var minIdleConnections = 1;
        var maxIdleConnections = 2;
        var maxTotalConnections = 3;
        var testConnectionOnBorrow = true;
        var testConnectionOnCreate = false;
        var testConnectionWhileIdle = true;
        var testConnectionOnReturn = false;
        var testQuery = "testquery";

        var commonsConnectionPoolConfig = CommonsConnectionPoolConfig.Builder.newInstance()
                .maxIdleConnections(maxIdleConnections)
                .maxTotalConnections(maxTotalConnections)
                .minIdleConnections(minIdleConnections)
                .testConnectionOnBorrow(testConnectionOnBorrow)
                .testConnectionOnCreate(testConnectionOnCreate)
                .testConnectionOnReturn(testConnectionOnReturn)
                .testConnectionWhileIdle(testConnectionWhileIdle)
                .testQuery(testQuery)
                .build();

        assertEquals(maxIdleConnections, commonsConnectionPoolConfig.getMaxIdleConnections());
        assertEquals(maxTotalConnections, commonsConnectionPoolConfig.getMaxTotalConnections());
        assertEquals(minIdleConnections, commonsConnectionPoolConfig.getMinIdleConnections());
        assertEquals(testConnectionOnBorrow, commonsConnectionPoolConfig.getTestConnectionOnBorrow());
        assertEquals(testConnectionOnCreate, commonsConnectionPoolConfig.getTestConnectionOnCreate());
        assertEquals(testConnectionOnReturn, commonsConnectionPoolConfig.getTestConnectionOnReturn());
        assertEquals(testConnectionWhileIdle, commonsConnectionPoolConfig.getTestConnectionWhileIdle());
        assertEquals(testQuery, commonsConnectionPoolConfig.getTestQuery());
    }
}
