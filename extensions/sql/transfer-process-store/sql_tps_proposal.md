## Table schema

```sql
create table edc.data_request
(
    id                varchar                                            not null
        constraint data_request_pk
            primary key,
    process_id        varchar                                            not null,
    connector_address varchar                                            not null,
    protocol          varchar default 'ids-multipart'::character varying not null,
    connector_id      varchar,
    asset_id          varchar                                            not null,
    contract_id       varchar                                            not null,
    data_destination  varchar                                            not null,
    managed_resources boolean default true,
    properties        varchar,
    transfer_type     varchar
);

comment on column edc.data_request.data_destination is 'DataAddress serialized as JSON';

comment on column edc.data_request.properties is 'java Map serialized as JSON';

comment on column edc.data_request.transfer_type is 'TransferType serialized as JSON';

alter table edc.data_request
    owner to "user";

create table edc.transfer_process
(
    id                       varchar           not null
        constraint transfer_process_pk
            primary key,
    type                     integer default 0 not null,
    state                    integer           not null,
    state_count              integer default 0 not null,
    state_time_stamp         timestamp,
    trace_context            varchar,
    error_detail             varchar,
    resource_manifest        varchar,
    provisioned_resource_set varchar,
    data_request_id          varchar           not null
        constraint data_request___fk
            references edc.data_request
            on update restrict on delete restrict
);

comment on column edc.transfer_process.trace_context is 'Java Map serialized as JSON';

comment on column edc.transfer_process.resource_manifest is 'java ResourceManifest serialized as JSON';

comment on column edc.transfer_process.provisioned_resource_set is 'ProvisionedResourceSet serialized as JSON';

alter table edc.transfer_process
    owner to "user";

create unique index transfer_process_id_uindex
    on edc.transfer_process (id);

create unique index data_request_id_uindex
    on edc.data_request (id);

```

## Translating the `TransferProcessStore` interface into SQL statements
#### `processIdForTransferId`
selects a TransferProcess based on the DataRequest ID
```sql
select t.* from edc.transfer_process t
where t.data_request_id=
      (select d.id
        from edc.data_request d
        where d.process_id = 'test-pid');
```

#### `find`
selects a TP based on its ID
```sql
select t.* 
from edc.transfer_process t
where t.id='test-id1';
```

#### `nextForState`
Selects the next batch of TP rows that are in a particular state and locks them. 
```sql
select * from edc.transfer_process where state <> 600 limit 5 for update skip locked ;
-- do TPM asynchronous stuff
update edc.transfer_process t set state = 800 where id='test-id2'
```
The `skip locked` statement will prevent any subsequent processes from blocking. It will most closely mimic the behaviour of the `lease`.

_Nota bene: this locks a row until the transaction is completed, see [below](#open-questions) for details._ 


#### `create`
inserts a new TP into the database by first inserting the `DataRequest`, obtaining it's ID and then creating the `TransferProcess` entry.
```sql
with drq_id as
    (insert into edc.data_request (id, process_id, connector_address, connector_id, asset_id, contract_id, data_destination, properties, transfer_type)
    values ('test-drq-2', 'test-pid2', 'http://anotherconnector.com', 'anotherconnector', 'asset2', 'contract2', '{}', null, default) returning id)

 insert into edc.transfer_process (id, state, state_time_stamp, trace_context, error_detail, resource_manifest, provisioned_resource_set, data_request_id)
    select 'test-id2', 400, now(), null, null, null, null, drq_id.id
    from drq_id;
```

#### `delete`
removes a TP from the database by first deleting the TP, then the DR:
```sql
with tid as (delete from edc.transfer_process where id='test-id2' returning transfer_process.data_request_id)
    delete from edc.data_request d where d.id = (select id from tid);
```

#### `update`
updates an existing TP
```sql
update edc.transfer_process t set state = 800 where id='test-id2'
```

## Create a flexible query API to accomodate `QuerySpec`

I recommend re-using the `Clause` interface and its implementors, that were originally implemented for CosmosDB, and create an equivalent set of clauses for SQL. Thus, there would be a `Limit-`, `Offset-`, `Order-` and `WhereClause` for SQL.

That way, we could even make dialect-dependent variants should the need arise, because the actual SQL statement is encoded in those clauses, offering a fluent Java API. 

We might have to tweak it a bit to allow nested statements (sub-selects).


## Open Questions<a name="open-questions"></a>
- `select...for update` only upholds the row lock until the transaction completes (cf. [docs](https://www.postgresql.org/docs/9.0/sql-select.html#SQL-FOR-UPDATE-SHARE)). That means transactions must be kept open until the TPM's async process returns and performs the update. 
If that is not desirable, I recommend adding a `Lease` table and recording leased rows explicitly, which will make it more explicit and human-readable, but will also make SQL statements and the table schema more complicated.