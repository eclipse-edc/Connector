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

package org.eclipse.dataspaceconnector.transfer.nifi;

/**
 * Configures a {@link NifiDataFlowController} instance.
 */
public class NifiTransferManagerConfiguration {
    private String url;
    private String flowUrl;

    private NifiTransferManagerConfiguration() {
    }

    public String getUrl() {
        return url;
    }

    public String getFlowUrl() {
        return flowUrl;
    }

    public static class Builder {
        private final NifiTransferManagerConfiguration configuration;

        private Builder() {
            configuration = new NifiTransferManagerConfiguration();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        /**
         * Sets the Nifi API URL.
         */
        Builder url(String url) {
            configuration.url = url;
            return this;
        }

        NifiTransferManagerConfiguration build() {
            return configuration;
        }

        public Builder flowUrl(String flowUrl) {
            configuration.flowUrl = flowUrl;
            return this;
        }
    }
}
