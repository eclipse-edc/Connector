package com.microsoft.dagx.transfer.nifi;

/**
 * Configures a {@link NifiDataFlowController} instance.
 */
public class NifiTransferManagerConfiguration {
    private String url;

    public String getUrl() {
        return url;
    }

    private NifiTransferManagerConfiguration() {
    }

    public static class Builder {
        private NifiTransferManagerConfiguration configuration;

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

        private Builder() {
            configuration = new NifiTransferManagerConfiguration();
        }
    }
}
