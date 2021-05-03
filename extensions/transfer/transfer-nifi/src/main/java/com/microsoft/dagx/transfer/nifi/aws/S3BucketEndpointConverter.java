package com.microsoft.dagx.transfer.nifi.aws;

import com.microsoft.dagx.spi.security.Vault;
import com.microsoft.dagx.spi.types.domain.transfer.DataAddress;
import com.microsoft.dagx.transfer.nifi.NifiTransferEndpoint;
import com.microsoft.dagx.transfer.nifi.NifiTransferEndpointConverter;
import kotlin.NotImplementedError;

public class S3BucketEndpointConverter implements NifiTransferEndpointConverter {
    public static final String TYPE = "S3Bucket";

    public S3BucketEndpointConverter(Vault vault) {

    }

    @Override
    public NifiTransferEndpoint convert(DataAddress dataAddress) {
        throw new RuntimeException("Converting DataAddresses to Amazon S3 bucket endpoints is not yet implemented!");
    }
}
