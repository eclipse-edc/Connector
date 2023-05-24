package org.eclipse.edc.sql.lease;

import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transaction.local.LocalTransactionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

class SqlLeaseContextTest {

    private final Monitor monitor = Mockito.mock(Monitor.class);
    private final TransactionContext transactionContext = new LocalTransactionContext(monitor);
    private final LeaseStatements leaseStatements = new TestEntityLeaseStatements();
    private final Clock clock = Clock.systemUTC();
    private final Duration leaseDuration = Duration.of(1, ChronoUnit.MINUTES);
    private final String leaseHolder = "test-lease-holder";
    private Connection connection;
    private PreparedStatement statement;
    private ResultSet resultSet;

    private SqlLeaseContext sqlLeaseContext;

    @BeforeEach
    void setUp() {
        connection = Mockito.mock(Connection.class);
        statement = Mockito.mock(PreparedStatement.class);
        resultSet = Mockito.mock(ResultSet.class);

        sqlLeaseContext = new SqlLeaseContext(transactionContext, leaseStatements, leaseHolder, clock, leaseDuration,
                connection);
    }

    @Test
    void givenNoLeasePresent_WhenAcquiringLease_ShouldAcquireLease() throws SQLException {
        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        when(connection.prepareStatement(queryCaptor.capture())).thenReturn(statement);
        when(connection.prepareStatement(queryCaptor.capture(), anyInt())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);

        assertDoesNotThrow(() -> sqlLeaseContext.acquireLease("test-entity"));
        List<String> capturedQueries = queryCaptor.getAllValues();
        assertEquals(3, capturedQueries.size());

        // trying to get the lease
        assertEquals(leaseStatements.getFindLeaseByEntityTemplate(), capturedQueries.get(0));

        // no lease found, so inserting one (no delete)
        assertEquals(leaseStatements.getInsertLeaseTemplate(), capturedQueries.get(1));

        // updating lease id in entity table
        assertEquals(leaseStatements.getUpdateLeaseTemplate(), capturedQueries.get(2));
    }

    @Test
    void givenOneLeasePresent_WhenAcquiringLease_ShouldDeleteOldLeaseAndAcquireNewLease() throws SQLException {
        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> paramCaptor = ArgumentCaptor.forClass(String.class);
        when(connection.prepareStatement(queryCaptor.capture())).thenReturn(statement);
        when(connection.prepareStatement(queryCaptor.capture(), anyInt())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        doNothing().when(statement).setString(anyInt(), paramCaptor.capture());
        when(resultSet.next()).thenReturn(true, false);
        String existingLeaseId = UUID.randomUUID().toString();
        when(resultSet.getString(anyString())).thenReturn(leaseHolder, existingLeaseId);
        when(resultSet.getLong(anyString())).thenReturn(clock.millis() - 60000L, 60000L);

        assertDoesNotThrow(() -> sqlLeaseContext.acquireLease("test-entity"));
        List<String> capturedQueries = queryCaptor.getAllValues();
        assertEquals(4, capturedQueries.size());

        // trying to get the lease
        assertEquals(leaseStatements.getFindLeaseByEntityTemplate(), capturedQueries.get(0));

        // one lease found, so deleting it
        assertEquals(leaseStatements.getDeleteLeaseTemplate(), capturedQueries.get(1));

        // inserting the new lease
        assertEquals(leaseStatements.getInsertLeaseTemplate(), capturedQueries.get(2));

        // updating lease id in entity table
        assertEquals(leaseStatements.getUpdateLeaseTemplate(), capturedQueries.get(3));

        List<String> capturedParams = paramCaptor.getAllValues();

        // delete query was fired with existing lease id
        assertEquals(existingLeaseId, capturedParams.get(1));
    }
}
