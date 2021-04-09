package com.microsoft.dagx.transfer.nifi;

import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.security.Vault;
import com.microsoft.dagx.spi.security.VaultResponse;
import com.microsoft.dagx.spi.transfer.flow.DataFlowInitiateResponse;
import com.microsoft.dagx.spi.transfer.response.ResponseStatus;
import com.microsoft.dagx.spi.types.TypeManager;
import com.microsoft.dagx.spi.types.domain.metadata.DataEntry;
import com.microsoft.dagx.spi.types.domain.metadata.DataEntryExtensions;
import com.microsoft.dagx.spi.types.domain.metadata.GenericDataEntryExtensions;
import com.microsoft.dagx.spi.types.domain.transfer.DataRequest;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NifiDataFlowControllerTest {

    private NifiDataFlowController controller;

    @BeforeEach
    void setUp() {
        Monitor monitor = new Monitor() {
        };
        NifiTransferManagerConfiguration config = NifiTransferManagerConfiguration.Builder.newInstance().url("https://gaiax-nifi.westeurope.cloudapp.azure.com")
                .build();
        TypeManager typeManager = new TypeManager();
        typeManager.registerTypes(DataRequest.class);
        Vault vault = new Vault() {
            @Override
            public @Nullable String resolveSecret(String key) {
                if (key.equals(NifiDataFlowController.NIFI_CREDENTIALS))
                    return "Basic cGF1bC5sYXR6ZWxzcGVyZ2VyQGJlYXJkeWluYy5jb206Q2JnR1RrdDh5LUY5NEJxMzhXb2g=";
                return null;
            }

            @Override
            public VaultResponse storeSecret(String key, String value) {
                return null;
            }

            @Override
            public VaultResponse deleteSecret(String key) {
                return null;
            }
        };
        controller = new NifiDataFlowController(config, typeManager, monitor, vault);
    }

    @Test
    void initiateFlow() {
        var ext = GenericDataEntryExtensions.Builder.newInstance().property("type", "AzureStorage")
                .property("account", "gxfilestore")
                .property("container", "yomama")
                .property("blobname", "bike.jpg")
                .property("sas", "?sv=2020-02-10&ss=b&srt=co&sp=rwdlacx&se=2021-04-09T18:18:24Z&st=2021-04-09T10:18:24Z&spr=https&sig=V5pxN2CLVIhWE9DSaJkAEOqaCDQEbKn2nyk4B34HczU%3D")
                .build();

        String id = UUID.randomUUID().toString();
        DataEntry<DataEntryExtensions> entry = DataEntry.Builder.newInstance().id(id).extensions(ext).build();

        DataRequest dataRequest = DataRequest.Builder.newInstance()
                .id(id)
                .dataEntry(entry)
                .dataTarget(AzureStorageTarget.Builder.newInstance()
                        .account("gxfilestore")
                        .container("yomama")
                        .blobName("bike_very_new.jpg")
                        .token("?sv=2020-02-10&ss=b&srt=co&sp=rwdlacx&se=2021-04-09T18:18:24Z&st=2021-04-09T10:18:24Z&spr=https&sig=V5pxN2CLVIhWE9DSaJkAEOqaCDQEbKn2nyk4B34HczU%3D")
                        .build())
                .build();

        //act
        DataFlowInitiateResponse response = controller.initiateFlow(dataRequest);

        //assert
        assertEquals(ResponseStatus.OK, response.getStatus());
    }
}
