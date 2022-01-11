package org.eclipse.dataspaceconnector.samples.sample042;

import org.eclipse.dataspaceconnector.spi.command.Command;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcessStates;

import java.time.Duration;

public class CheckTransferProcessTimeoutCommand extends Command {

    private final int batchSize;
    private final TransferProcessStates targetState;
    private final Duration maxAge;

    public CheckTransferProcessTimeoutCommand(int batchSize, TransferProcessStates targetState, Duration maxAge) {
        this.batchSize = batchSize;
        this.targetState = targetState;
        this.maxAge = maxAge;
    }

    public int getBatchSize() {
        return batchSize;
    }


    public TransferProcessStates getTargetState() {
        return targetState;
    }

    public Duration getMaxAge() {
        return maxAge;
    }
}
