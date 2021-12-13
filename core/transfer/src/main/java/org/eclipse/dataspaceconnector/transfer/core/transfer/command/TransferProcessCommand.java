package org.eclipse.dataspaceconnector.transfer.core.transfer.command;

public abstract class TransferProcessCommand {

    private final String id;

    protected TransferProcessCommand(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}