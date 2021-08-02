/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.transfer.store.memory;

import com.microsoft.dagx.spi.types.domain.transfer.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.niceMock;
import static org.easymock.EasyMock.replay;
import static org.junit.jupiter.api.Assertions.*;

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

        assertNotNull(store.processIdForTransferId("clientid"));

        assertEquals(TransferProcessStates.INITIAL.code(), found.getState());

        transferProcess.transitionProvisioning(ResourceManifest.Builder.newInstance().build());

        store.update(transferProcess);

        found = store.find(id);
        assertNotNull(found);
        assertEquals(TransferProcessStates.PROVISIONING.code(), found.getState());

        store.delete(id);
        Assertions.assertNull(store.find(id));
        assertNull(store.processIdForTransferId("clientid"));

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

    @Test
    void verifyMutlipleRequets() {
        String id1 = UUID.randomUUID().toString();
        TransferProcess transferProcess1 = TransferProcess.Builder.newInstance().id(id1).dataRequest(DataRequest.Builder.newInstance().id("clientid1").destinationType("test").build()).build();
        store.create(transferProcess1);

        String id2 = UUID.randomUUID().toString();
        TransferProcess transferProcess2 = TransferProcess.Builder.newInstance().id(id2).dataRequest(DataRequest.Builder.newInstance().id("clientid2").destinationType("test").build()).build();
        store.create(transferProcess2);


        TransferProcess found1 = store.find(id1);
        assertNotNull(found1);

        TransferProcess found2 = store.find(id2);
        assertNotNull(found2);

        var found = store.nextForState(TransferProcessStates.INITIAL.code(), 3);
        assertEquals(2, found.size());

    }

    @Test
    void verifyOrderingByTimestamp() {
        for (int i = 0; i < 100; i++) {
            final TransferProcess process = createProcess("test-process-" + i);
            store.create(process);
        }

        final List<TransferProcess> processes = store.nextForState(TransferProcessStates.INITIAL.code(), 50);

        assertThat(processes).hasSize(50);
        assertThat(processes).allMatch(p -> p.getStateTimestamp() > 0);
    }

    @Test
    void verifyNextForState_avoidsStarvation() throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            final TransferProcess process = createProcess("test-process-" + i);
            store.create(process);
        }

        var list1 = store.nextForState(TransferProcessStates.INITIAL.code(), 5);
        Thread.sleep(50); //simulate a short delay to generate different timestamps
        list1.forEach(tp -> store.update(tp));
        var list2 = store.nextForState(TransferProcessStates.INITIAL.code(), 5);
        assertThat(list1).isNotEqualTo(list2).doesNotContainAnyElementsOf(list2);
    }

    private TransferProcess createProcess(String name) {
        final DataRequest mock = niceMock(DataRequest.class);
        replay(mock);
        return TransferProcess.Builder.newInstance()
                .type(TransferProcess.Type.CLIENT)
                .id(name)
                .stateTimestamp(0)
                .state(TransferProcessStates.UNSAVED.code())
                .provisionedResourceSet(new ProvisionedResourceSet())
                .dataRequest(mock)
                .build();
    }


    @BeforeEach
    void setUp() {
        store = new InMemoryTransferProcessStore();
    }
}
