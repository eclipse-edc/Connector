# SQL-based `TransferProcessStore` - technical proposal

**_Note that the SQL statements (DDL and DML) are specific to and only tested with PostgreSQL. Using it with other RDBMS
may work but might have unexpected side effects!_**

## Table schema DDL

please refer to [ddl_postgres.sql](./ddl_postgres.sql):

## Translating the `TransferProcessStore` interface into SQL statements

all SQL snippets taken from [dml_postgres.sql](dml_postgres.sql)

#### `processIdForTransferId`

selects a TransferProcess based on the DataRequest ID

```sql
select t.*
from edc_transfer_process t
where t.id =
      (select d.edc_transfer_process_id
       from edc_data_request d
       where d.process_id = 'test-pid2');
```

#### `find`

selects a TP based on its ID

```sql
select t.*
from edc_transfer_process t
where t.id = 'test-id1';
```

#### `nextForState`

Selects the next batch of TP rows that are in a particular state and leases/locks them. Please note that in the current
scheme selecting the candidates, creating lease entries and updating the candidates are three distinct DB operations
that **must** run in the same transaction:

```sql
-- next for state: 3 statements to select and write lease
-- select TPs with no or expired lease
select *
from edc_transfer_process
where lease_id is null
   or lease_id in (select lease_id from edc_lease where (NOW > (leased_at + edc_lease.lease_duration));

-- create lease
insert into edc_lease (lease_id, leased_by, leased_at, lease_duration)
values ('lease-1', 'yomama', NOW), default);

-- lease selected TPs, provide list of IDs
update edc_transfer_process t
set lease_id='lease-1'
where id in ('test-id2');
```

_Note: `NOW` is not a variable, it should be passed to the statement as parameter._

in Java (pseudo-)code this could look something like this:

```java
import java.time.Instant;

public class SqlTransferProcessStore {

    //...

    public List<TransferProcess> nextForState(int state, int batchSize) {
        txMgr.executeTransaction(() -> {

            var transferProcessList = selectByState(state, batchSize); // must skip currently leased ones

            transferProcessList.forEach(this::acquireLease);
        });
    }

    private void acquireLease(TransferProcess process) {
        var lease = createLease(Instant.now().toEpochMilli(), connectorId); //create lease in DB, returns object
        writeLease(process.getId(), lease.getId()); //UPDATE edc_transfer_process SET lease_id=ID where id=TPID
    }
}
```

Ideally, the implementation of the `Lease` mechanism is kept generic, so that it can be re-used in other situations,
such as the SQL-`ContractNegotiationStore`.

#### `create`

inserts a new TP into the database by first creating the `TransferProcess`, and using its ID to create the `DataRequest`
entry with the FK referencing `edc_transfer_process` to enable `ON DELETE CASCADE`.

```sql
--must be done in the same transaction:

insert into edc_transfer_process (id, state, state_time_stamp, trace_context, error_detail, resource_manifest,
                              provisioned_resource_set)
values ('test-id2', 400, now(), null, null, null, null);


insert into edc_data_request (id, process_id, connector_address, connector_id, asset_id, contract_id, data_destination,
                          properties, transfer_type, edc_transfer_process_id)
values ('test-drq-2', 'test-pid2', 'http://anotherconnector.com', 'anotherconnector', 'asset2', 'contract2', '{}', null,
        default, 'test-id2');
```

#### `delete`

removes a TP from the database by deleting the TP cascading to the DR:

```sql
-- fk delete cascade will remove the edc_data_request row
delete
from edc_transfer_process
where id = 'test-id2';
```

#### `update`

updates an existing TP and "breaks" the lease

```sql
-- first break lease, automatically nulls edc_transfer_process.lease_id
delete
from edc_lease l
where l.lease_id = (select lease_id from edc_transfer_process where id = 'test-id2');

-- then update the edc_transfer_process
update edc_transfer_process t
set state   = 800,
    lease_id=null
where id = 'test-id2';
```

## Create a flexible query API to accommodate `QuerySpec`

_For the first version, it is sufficient to use the `limit` and `offset` arguments from the `QuerySpec`._

I recommend re-using the `Clause` interface and its implementors, that were originally implemented for CosmosDB, and
create an equivalent set of clauses for SQL. Thus, there would be a `Limit-`, `Offset-`, `Order-` and `WhereClause` for
SQL.

That way, we could even make dialect-dependent variants should the need arise, because the actual SQL statement is
encoded in those clauses, offering a fluent Java API.

We might have to tweak it a bit to allow nested statements (sub-selects).