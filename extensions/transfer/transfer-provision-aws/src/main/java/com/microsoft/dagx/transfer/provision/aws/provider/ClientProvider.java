package com.microsoft.dagx.transfer.provision.aws.provider;

import software.amazon.awssdk.core.SdkClient;

/**
 * Provides reusable S3 clients that are configured to connect to specific regions and endpoints.
 */
public interface ClientProvider {

    /**
     * Returns the client of the specified type for the region id or endpoint URI.
     */
    <T extends SdkClient> T clientFor(Class<T> type, String key);

}
