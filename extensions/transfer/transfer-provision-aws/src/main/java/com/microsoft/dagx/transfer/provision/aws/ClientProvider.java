package com.microsoft.dagx.transfer.provision.aws;

import software.amazon.awssdk.services.s3.S3AsyncClient;

/**
 * Provides reusable S3 clients that are configured to connect to specific regions and endpoints.
 */
@FunctionalInterface
public interface ClientProvider {

    /**
     * Returns the S3 client for the region id or endpoint URI.
     */
    S3AsyncClient clientFor(String key);

}
