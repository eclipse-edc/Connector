package com.microsoft.dagx.transfer.nifi.azureblob;

import com.microsoft.dagx.spi.security.Vault;
import com.microsoft.dagx.spi.types.domain.transfer.DataAddress;
import com.microsoft.dagx.transfer.nifi.NifiTransferEndpointConverter;

public class AzureEndpointConverter implements NifiTransferEndpointConverter {
    public static final String TYPE = AzureTransferEndpoint.TYPE_AZURE_STORAGE;
    private final Vault vault;
    private static final String PROPERTY_ACCOUNT = "account";
    private static final String PROPERTY_BLOBNAME = "blobname";
    private static final String PROPERTY_CONTAINER = "container";

    public AzureEndpointConverter(Vault vault) {
        this.vault = vault;
    }

    @Override
    public AzureTransferEndpoint convert(DataAddress dataAddress) {
        var key = vault.resolveSecret(dataAddress.getKeyName());
        return AzureTransferEndpoint.Builder.anAzureTransferEndpoint()
                .withAccount(dataAddress.getProperty(PROPERTY_ACCOUNT))
                .withBlobName(dataAddress.getProperty(PROPERTY_BLOBNAME))
                .withContainerName(dataAddress.getProperty(PROPERTY_CONTAINER))
                .withKey(key)
                .build();
    }
}
