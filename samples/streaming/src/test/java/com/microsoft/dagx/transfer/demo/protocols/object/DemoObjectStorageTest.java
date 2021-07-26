package com.microsoft.dagx.transfer.demo.protocols.object;

import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.transfer.demo.protocols.common.DataDestination;
import com.microsoft.dagx.transfer.demo.protocols.spi.object.ObjectStorageObserver;
import org.easymock.EasyMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 */
class DemoObjectStorageTest {

    private DemoObjectStorage objectStore;

    @Test
    void verifyStorage() throws Exception {
        ObjectStorageObserver observer = EasyMock.createMock(ObjectStorageObserver.class);
        observer.onProvision(EasyMock.isA(DataDestination.class));
        observer.onStore(EasyMock.isA(String.class), EasyMock.isA(String.class), EasyMock.isA(String.class), EasyMock.isA(byte[].class));
        observer.onDeprovision(EasyMock.isA(String.class));
        EasyMock.expectLastCall();
        EasyMock.replay(observer);

        objectStore.register(observer);

        objectStore.start();
        var destination = objectStore.provision("test").get();

        assertEquals("test", destination.getDestinationName());

        objectStore.store("test", "data1", destination.getAccessToken(), "test".getBytes());

        objectStore.deprovision("test");

        EasyMock.verify(observer);
    }

    @BeforeEach
    void setUp() {
        objectStore = new DemoObjectStorage(new Monitor() {
        });
        objectStore.setProvisionWait(1);
    }

    @AfterEach
    void tearDown() {
        objectStore.stop();
    }
}
