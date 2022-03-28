package org.eclipse.dataspaceconnector.transfer.dataplane.spi;

/**
 * Type of Data Plane transfer.
 */
public interface DataPlaneTransferType {
    /**
     * Synchronous data transfer using public API of the Data Plane as http proxy to query the data.
     */
    String SYNC = "HttpProxy";
}
