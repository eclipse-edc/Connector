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

package org.eclipse.dataspaceconnector.clients.postgresql.connection.pool.commons;

public final class CommonsConnectionPoolStats {

    private final int active;
    private final int idle;
    private final int waiters;

    private CommonsConnectionPoolStats(int active, int idle, int waiters) {
        this.active = active;
        this.idle = idle;
        this.waiters = waiters;
    }

    public int getActive() {
        return active;
    }

    public int getIdle() {
        return idle;
    }

    public int getWaiters() {
        return waiters;
    }

    @Override
    public String toString() {
        return "CommonsConnectionPoolStats{" +
                "active=" + active +
                ", idle=" + idle +
                ", waiters=" + waiters +
                '}';
    }

    public static class Builder {
        private int active;
        private int idle;
        private int waiters;

        private Builder() {
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder active(int active) {
            this.active = active;
            return this;
        }

        public Builder idle(int idle) {
            this.idle = idle;
            return this;
        }

        public Builder waiters(int waiters) {
            this.waiters = waiters;
            return this;
        }

        public CommonsConnectionPoolStats build() {
            return new CommonsConnectionPoolStats(active, idle, waiters);
        }
    }
}
