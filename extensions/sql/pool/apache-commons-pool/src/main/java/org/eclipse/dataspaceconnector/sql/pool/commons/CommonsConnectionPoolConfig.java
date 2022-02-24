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
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.sql.pool.commons;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * A CommonsConnectionPoolConfig is a container object containing a set of pool configuration
 * parameters that can be used by the {@code CommonsConnectionPool} for connection management.
 */
class CommonsConnectionPoolConfig {

    private final int maxIdleConnections;
    private final int maxTotalConnections;
    private final int minIdleConnections;
    private final boolean testConnectionOnBorrow;
    private final boolean testConnectionOnCreate;
    private final boolean testConnectionOnReturn;
    private final boolean testConnectionWhileIdle;
    private final String testQuery;

    private CommonsConnectionPoolConfig(
            int maxIdleConnections,
            int maxTotalConnections,
            int minIdleConnections,
            boolean testConnectionOnBorrow,
            boolean testConnectionOnCreate,
            boolean testConnectionOnReturn,
            boolean testConnectionWhileIdle,
            @NotNull String testQuery) {
        this.maxIdleConnections = maxIdleConnections;
        this.maxTotalConnections = maxTotalConnections;
        this.minIdleConnections = minIdleConnections;
        this.testConnectionOnBorrow = testConnectionOnBorrow;
        this.testConnectionOnCreate = testConnectionOnCreate;
        this.testConnectionOnReturn = testConnectionOnReturn;
        this.testConnectionWhileIdle = testConnectionWhileIdle;
        this.testQuery = Objects.requireNonNull(testQuery);
    }

    public int getMaxIdleConnections() {
        return maxIdleConnections;
    }

    public int getMaxTotalConnections() {
        return maxTotalConnections;
    }

    public int getMinIdleConnections() {
        return minIdleConnections;
    }

    public boolean getTestConnectionOnBorrow() {
        return testConnectionOnBorrow;
    }

    public boolean getTestConnectionOnCreate() {
        return testConnectionOnCreate;
    }

    public boolean getTestConnectionOnReturn() {
        return testConnectionOnReturn;
    }

    public boolean getTestConnectionWhileIdle() {
        return testConnectionWhileIdle;
    }

    public String getTestQuery() {
        return testQuery;
    }

    public static final class Builder {
        private int maxIdleConnections = 4;
        private int maxTotalConnections = 8;
        private int minIdleConnections = 1;
        private boolean testConnectionOnBorrow = true;
        private boolean testConnectionOnCreate = true;
        private boolean testConnectionOnReturn = true;
        private boolean testConnectionWhileIdle = false;
        private String testQuery = "SELECT 1;";

        private Builder() {
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder maxIdleConnections(int maxIdleConnections) {
            this.maxIdleConnections = maxIdleConnections;
            return this;
        }

        public Builder maxTotalConnections(int maxTotalConnections) {
            this.maxTotalConnections = maxTotalConnections;
            return this;
        }

        public Builder minIdleConnections(int minIdleConnections) {
            this.minIdleConnections = minIdleConnections;
            return this;
        }

        public Builder testConnectionOnBorrow(boolean testConnectionOnBorrow) {
            this.testConnectionOnBorrow = testConnectionOnBorrow;
            return this;
        }

        public Builder testConnectionOnCreate(boolean testConnectionOnCreate) {
            this.testConnectionOnCreate = testConnectionOnCreate;
            return this;
        }

        public Builder testConnectionOnReturn(boolean testConnectionOnReturn) {
            this.testConnectionOnReturn = testConnectionOnReturn;
            return this;
        }

        public Builder testConnectionWhileIdle(boolean testConnectionWhileIdle) {
            this.testConnectionWhileIdle = testConnectionWhileIdle;
            return this;
        }

        public Builder testQuery(String testQuery) {
            this.testQuery = testQuery;
            return this;
        }

        public CommonsConnectionPoolConfig build() {
            return new CommonsConnectionPoolConfig(
                    maxIdleConnections,
                    maxTotalConnections,
                    minIdleConnections,
                    testConnectionOnBorrow,
                    testConnectionOnCreate,
                    testConnectionOnReturn,
                    testConnectionWhileIdle,
                    testQuery
            );
        }
    }
}
