package org.eclipse.dataspaceconnector.transfer.core;

public class TransferProcessConfiguration {
    private final int stateTimeoutThreshold;
    private final int stateCountThreshold;

    public TransferProcessConfiguration(int stateTimeoutThreshold, int stateCountThreshold) {

        this.stateTimeoutThreshold = stateTimeoutThreshold;
        this.stateCountThreshold = stateCountThreshold;
    }

    public int getStateCountThreshold() {
        return stateCountThreshold;
    }

    public int getStateTimeoutThreshold() {
        return stateTimeoutThreshold;
    }
}
