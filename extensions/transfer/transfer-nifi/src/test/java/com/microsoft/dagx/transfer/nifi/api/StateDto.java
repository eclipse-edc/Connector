package com.microsoft.dagx.transfer.nifi.api;

public class StateDto {
    public String id;
    public String state;
    public boolean disconnectedNodeAcknowledged;

    public StateDto(String processGroup, String state, boolean disconnectedNodeAck) {
        this.id = processGroup;
        this.state = state;
        this.disconnectedNodeAcknowledged = disconnectedNodeAck;
    }

    public StateDto() {
    }
}
