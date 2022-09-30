# Sql query streaming

## Decision

We decided to switch the current way we are using to retrieve objects from sql query - that fetches everything in memory 
and then creates a `Stream` out of the resulting `List` - to a version where a `Stream` is actually used to iterate 
over the `ResultSet`.

## Rationale

Now that the EDC is being used by teams that are pushing its limits with heavy workloads we noticed, especially on catalog request,
that a lot of time is wasted fetching all the rows in memory and then opening a stream later on.
A `Spliterator` could be used to create a stream that actually fetches items lazily.


## Approach

To obtain streaming we would add a method in the `SqlQueryExecutor` that creates a stream with a `Spliterator` that iterates on the `ResultSet`.

Here are some code snippets starting from the bottom to the top:

The `SqlQueryExecutor.executeQueryStream` method:
(note: `Connection`/`Statement`/`ResultSet` needs to be closed when the stream has been closed (this event is caught by the `onClose` method)

```java
public final class SqlQueryExecutor {

    ...

    public static <T> Stream<T> executeQueryStream(Connection connection, ResultSetMapper<T> resultSetMapper, String sql, Object... arguments) {
        try {
            var statement = connection.prepareStatement(sql);
            statement.setFetchSize(5000);
            setArguments(statement, arguments);
            var resultSet = statement.executeQuery();
            return StreamSupport.stream(new Spliterators.AbstractSpliterator<T>(Long.MAX_VALUE, Spliterator.ORDERED) {
                @Override
                public boolean tryAdvance(Consumer<? super T> action) {
                    try {
                        if (!resultSet.next()) {
                            return false;
                        }
                        action.accept(resultSetMapper.mapResultSet(resultSet));
                        return true;
                    } catch (Exception ex) {
                        throw new EdcPersistenceException(ex);
                    }
                }

            }, false).onClose(() -> {
                // close connection/statement/resultSet    
            });
        } catch (SQLException sqlEx) {
            // close connection/statement/resultSet if opened

            throw new EdcPersistenceException(sqlEx);
        }
    }
    
    ...
}
```

A caller of the `executeQueryStream` method, please note that the `Connection` must not be closed by the caller as it's done
currently with the `try-as-resources` statement.

```java
public class SqlContractDefinitionStore implements ContractDefinitionStore {
    
    ...
    
    @Override
    public @NotNull Stream<ContractDefinition> findAll(QuerySpec spec) {
        return transactionContext.execute(() -> {
            // try (var connection = getConnection()) { // this is the old way, not needed now as the connection will be closed by the stream itself
            try {
                var connection = getConnection();
                var queryStmt = statements.createQuery(spec);
                return executeQueryStream(connection, this::mapResultSet, queryStmt.getQueryAsString(), queryStmt.getParameters());
            } catch (SQLException exception) {
                throw new EdcPersistenceException(exception);
            }
        });
    }
    
    ...
    
}
```

One big news is that at the top of the call, the stream needs to be closed explicitly to avoid connection leaks, as
neither `collect` nor `forEach` do that:

```java
public class PolicyDefinitionServiceImpl implements PolicyDefinitionService {
    ...
    
    @Override
    public @NotNull ServiceResult<PolicyDefinition> deleteById(String policyId) {
        
        ...

        try (var contractDefinitionOnPolicy = contractDefinitionStore.findAll(queryContractPolicyFilter)) { // this will close the stream after the use
            if (contractDefinitionOnPolicy.findAny().isPresent()) {
                return ServiceResult.conflict(format("PolicyDefinition %s cannot be deleted as it is referenced by at least one contract definition", policyId));
            }
        }
        
        ...
    
    }

}
```

This `executeQueryStream` method will likely be used by all the `.findAll` methods of the stores.