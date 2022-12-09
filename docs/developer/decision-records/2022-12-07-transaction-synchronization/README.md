# Transaction Synchronization

## Decision

The EDC `TransactionContext` will add the ability to register transaction synchronizations to receive callbacks prior to when a transaction is committed or rolled back.

## Rationale

Transaction synchronization is a well-established pattern (.cf JTA) and will be used to automatically close resources and reduce the risk of runtime resource leaks. For example,
`SqlQueryExecutor` will use a transaction synchronization to close JDBC resources associated with streams returned from the `executeQuery` method:

`
public static <T> Stream<T> executeQuery(Connection connection, boolean closeConnection, ResultSetMapper<T> resultSetMapper, String sql, Object... arguments)
`

## Approach

Synchronizations must implement the `TransactionSynchronization` interface:

`
@FunctionalInterface
interface TransactionSynchronization {
void beforeCompletion();
}
`
Transaction synchronizations are registered using the `registerSynchronization` method on `TransactionContext` and will be associated with the active transaction context associated
with the current thread:

`
void registerSynchronization(TransactionSynchronization sync);
`

Transaction synchronizations must only be registered when a transaction is active and will be cleared after the transaction commits or is rolled back. Transaction synchronizations
will be supported across all `TransactionContext` implementations. 
