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

package org.eclipse.dataspaceconnector.transaction.atomikos;

import com.atomikos.datasource.xa.XID;

import java.util.Arrays;
import java.util.Objects;

/**
 * Configuration for the Atomikos transaction manager.
 */
public class TransactionManagerConfiguration {
    private String name;
    private String dataDir;
    private int timeout = -1;  // -1 indicates the default Atomikos timeout.
    private boolean singleThreaded2Pc = false;
    private boolean enableLogging = true;
    private int checkPointInterval = -1;

    public String getName() {
        return name;
    }

    public String getDataDir() {
        return dataDir;
    }

    public int getTimeout() {
        return timeout;
    }

    public boolean getSingleThreaded2Pc() {
        return singleThreaded2Pc;
    }

    public boolean getEnableLogging() {
        return enableLogging;
    }

    public int getCheckPointInterval() {
        return checkPointInterval;
    }

    private TransactionManagerConfiguration() {
    }

    public static class Builder {
        private TransactionManagerConfiguration configuration;

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder name(String name) {
            // shorten to length required by Atomikos if too large
            if (name.getBytes().length + 16 > XID.MAXGTRIDSIZE) {
                name = new String(Arrays.copyOfRange(name.getBytes(), 0, XID.MAXGTRIDSIZE - 16));
            }
            configuration.name = name;
            return this;
        }

        public Builder dataDir(String dir) {
            configuration.dataDir = dir;
            return this;
        }

        public Builder singleThreaded2Pc(boolean singleThreaded2Pc) {
            configuration.singleThreaded2Pc = singleThreaded2Pc;
            return this;
        }

        public Builder enableLogging(boolean enableLogging) {
            configuration.enableLogging = enableLogging;
            return this;
        }

        public Builder checkPointInterval(int checkPointInterval) {
            configuration.checkPointInterval = checkPointInterval;
            return this;
        }

        public Builder timeout(int timeout) {
            configuration.timeout = timeout;
            return this;
        }

        public TransactionManagerConfiguration build() {
            Objects.requireNonNull(configuration.name);
            return configuration;
        }

        private Builder() {
            configuration = new TransactionManagerConfiguration();
        }
    }
}
