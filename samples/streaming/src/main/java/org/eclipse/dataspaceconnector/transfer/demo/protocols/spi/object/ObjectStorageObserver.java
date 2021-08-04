package org.eclipse.dataspaceconnector.transfer.demo.protocols.spi.object;

import org.eclipse.dataspaceconnector.transfer.demo.protocols.common.DataDestination;

/**
 * Observes storage operations.
 */
public interface ObjectStorageObserver {

    /**
     * Callback when a provision operation succeeds.
     */
    void onProvision(DataDestination dataDestination);

    /**
     * Callback when a deprovision operation completes.
     */
    void onDeprovision(String key);

    /**
     * Callback when a storage operation is invoked.
     */
    void onStore(String containerName, String objectKey, String token, byte[] data);

}
