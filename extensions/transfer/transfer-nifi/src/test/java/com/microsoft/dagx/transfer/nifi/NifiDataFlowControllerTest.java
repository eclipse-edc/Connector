package com.microsoft.dagx.transfer.nifi;

import com.microsoft.dagx.spi.DagxException;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.security.Vault;
import com.microsoft.dagx.spi.transfer.flow.DataFlowInitiateResponse;
import com.microsoft.dagx.spi.transfer.response.ResponseStatus;
import com.microsoft.dagx.spi.types.TypeManager;
import com.microsoft.dagx.spi.types.domain.metadata.DataEntry;
import com.microsoft.dagx.spi.types.domain.metadata.DataEntryExtensions;
import com.microsoft.dagx.spi.types.domain.metadata.GenericDataEntryExtensions;
import com.microsoft.dagx.spi.types.domain.transfer.DataRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.easymock.EasyMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NifiDataFlowControllerTest {

    private NifiDataFlowController controller;
    private Vault vault;


    @BeforeEach
    void setUp() {
        Monitor monitor = new Monitor() {
        };
        NifiTransferManagerConfiguration config = NifiTransferManagerConfiguration.Builder.newInstance().url("https://gaiax-nifi.westeurope.cloudapp.azure.com")
                .build();
        TypeManager typeManager = new TypeManager();
        typeManager.registerTypes(DataRequest.class);

        vault= createMock(Vault.class);
        expect(vault.resolveSecret(NifiDataFlowController.NIFI_CREDENTIALS)).andReturn("Basic cGF1bC5sYXR6ZWxzcGVyZ2VyQGJlYXJkeWluYy5jb206Q2JnR1RrdDh5LUY5NEJxMzhXb2g=");
        replay(vault);
        controller = new NifiDataFlowController(config, typeManager, monitor, vault);
    }

    @Test
    @Disabled("disabled until https://github.com/microsoft/Data-Appliance-GX/issues/30 is done")
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

    @Test
    void initiateFlow_noTargetDefined(){
        String id = UUID.randomUUID().toString();
        DataEntry<DataEntryExtensions> entry = DataEntry.Builder.newInstance().extensions(GenericDataEntryExtensions.Builder.newInstance().build()).build();

        DataRequest dataRequest = DataRequest.Builder.newInstance()
                .id(id)
                .dataEntry(entry)
                .build();

       var e= assertThrows(DagxException.class, ()-> controller.initiateFlow(dataRequest));
       assertEquals(IllegalArgumentException.class, e.getCause().getClass());
    }

    @Test
    void initiateFlow_noCredsFoundInVault(){
        String id = UUID.randomUUID().toString();
        DataEntry<DataEntryExtensions> entry = DataEntry.Builder.newInstance().extensions(GenericDataEntryExtensions.Builder.newInstance().build()).build();

        DataRequest dataRequest = DataRequest.Builder.newInstance()
                .id(id)
                .dataTarget(() -> "TestType")
                .dataEntry(entry)
                .build();

        reset(vault);
        expect(vault.resolveSecret(NifiDataFlowController.NIFI_CREDENTIALS)).andReturn(null);
        replay(vault);

        assertThrows(DagxException.class, () -> controller.initiateFlow(dataRequest), "No NiFi credentials found in Vault!");
    }
}
