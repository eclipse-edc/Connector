package org.eclipse.edc.transaction.spi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class NoopTransactionContextTest {
    private NoopTransactionContext transactionContext;

    @Test
    void verifySynchronization() {
        var sync = mock(TransactionContext.TransactionSynchronization.class);

        // the sync should be invoked
        transactionContext.execute(() -> transactionContext.registerSynchronization(sync));

        // the sync should be cleared and should not be invoked again
        transactionContext.execute(() -> {
        });

        verify(sync, times(1)).beforeCompletion();
    }

    @BeforeEach
    void setUp() {
        transactionContext = new NoopTransactionContext();
    }
}
