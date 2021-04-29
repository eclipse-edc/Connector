package com.microsoft.dagx.transfer.store.memory;

import com.microsoft.dagx.spi.types.domain.transfer.DataRequest;
import com.microsoft.dagx.spi.types.domain.transfer.ResourceManifest;
import com.microsoft.dagx.spi.types.domain.transfer.TransferProcess;
import com.microsoft.dagx.spi.types.domain.transfer.TransferProcessStates;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 */
class InMemoryTransferProcessStoreTest {
    private InMemoryTransferProcessStore store;

    @Test
    void verifyCreateUpdateDelete() {
        String id = UUID.randomUUID().toString();
        TransferProcess transferProcess = TransferProcess.Builder.newInstance().id(id).dataRequest(DataRequest.Builder.newInstance().id("clientid").destinationType("test").build()).build();

        store.create(transferProcess);

        TransferProcess found = store.find(id);

        assertNotNull(found);
        assertNotSame(found, transferProcess); // enforce by-value

        assertTrue(store.externalIdReceived("clientid"));

        assertEquals(TransferProcessStates.INITIAL.code(), found.getState());

        transferProcess.transitionProvisioning(ResourceManifest.Builder.newInstance().build());

        store.update(transferProcess);

        found = store.find(id);
        assertNotNull(found);
        assertEquals(TransferProcessStates.PROVISIONING.code(), found.getState());

        store.delete(id);
        Assertions.assertNull(store.find(id));
        assertFalse(store.externalIdReceived("clientid"));

    }

    @Test
    void verifyNext() throws InterruptedException {
        String id1 = UUID.randomUUID().toString();
        TransferProcess transferProcess1 = TransferProcess.Builder.newInstance().id(id1).dataRequest(DataRequest.Builder.newInstance().id("clientid").destinationType("test").build()).build();
        String id2 = UUID.randomUUID().toString();
        TransferProcess transferProcess2 = TransferProcess.Builder.newInstance().id(id2).dataRequest(DataRequest.Builder.newInstance().id("clientid").destinationType("test").build()).build();

        store.create(transferProcess1);
        store.create(transferProcess2);

        transferProcess2.transitionProvisioning(ResourceManifest.Builder.newInstance().build());
        store.update(transferProcess2);
        Thread.sleep(1);
        transferProcess1.transitionProvisioning(ResourceManifest.Builder.newInstance().build());
        store.update(transferProcess1);

        assertTrue(store.nextForState(TransferProcessStates.INITIAL.code(), 1).isEmpty());

        List<TransferProcess> found = store.nextForState(TransferProcessStates.PROVISIONING.code(), 1);
        assertEquals(1, found.size());
        assertEquals(transferProcess2, found.get(0));

        found = store.nextForState(TransferProcessStates.PROVISIONING.code(), 3);
        assertEquals(2, found.size());
        assertEquals(transferProcess2, found.get(0));
        assertEquals(transferProcess1, found.get(1));
    }

    @BeforeEach
    void setUp() {
        store = new InMemoryTransferProcessStore();
    }
}
