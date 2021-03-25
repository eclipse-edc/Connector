package com.microsoft.dagx.spi.types.domain.transfer;

/**
 * A data transfer process.
 */
public class TransferProcess {
    private String id;
    private DataRequest dataRequest;
    private int version;

    private int state;
    private int stateCount;
    private long stateTimestamp;

    public String getId() {
        return id;
    }

    public DataRequest getDataRequest() {
        return dataRequest;
    }

    public int getVersion() {
        return version;
    }

    public int getState() {
        return state;
    }

    public int getStateCount() {
        return stateCount;
    }

    public long getStateTimestamp() {
        return stateTimestamp;
    }
}
