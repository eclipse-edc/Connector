package org.eclipse.dataspaceconnector.spi.transaction;

/**
 * Default implementation for TransactionContext, to be used only for testing/sampling purposes
 */
public class NoopTransactionContext implements TransactionContext {
    @Override
    public void execute(TransactionBlock block) {
        block.execute();
    }
}
