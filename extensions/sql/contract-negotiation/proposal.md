# SQL-based `ContractNegotiationStore` - technical proposal

## 1. Table schema
see [ddl.sql](ddl.sql).

As an alternative to storing `ContractAgreement`s in a dedicated table, it could also be serialized and stored as column 
in the `contract_negotiation` table. However, we will need to be able to list all contract agreements at some point, so it 
seemed more future-proof to have it separate.

## 2. Translating the `ContractNegotiationStore` into SQL statements

see [dml.sql](dml.sql)

### `find`

```sql
SELECT *
FROM contract_negotiation
WHERE id = 'test-cn1';

```

### `findForCorrelationId`
```sql
SELECT *
FROM contract_negotiation
WHERE correlation_id = 'corr1';
```

### `findContractAgreement`
```sql
SELECT *
FROM contract_agreement
WHERE id = 'contract1';
```

### `save`
Saving an entity will have "upsert" semantics, and break the lease if updated.

```sql
-- if not exist create
INSERT INTO contract_negotiation (id, correlation_id, counterparty_id, counterparty_address)
VALUES ('test-cn1', 'corr1', 'other-id', 'http://other.com');

-- if exist update
DELETE
FROM lease
WHERE lease_id = (SELECT lease_id FROM contract_negotiation WHERE id = 'test-cn2');

UPDATE contract_negotiation
SET state=200, state_count=3, state_timestamp=12345, error_detail=NULL, contract_offers='{}', trace_context='{}'
WHERE id='test-cn1';

```

### `delete`
Will only delete if there is no contract yet.
```sql
DELETE
FROM contract_negotiation
WHERE id = 'test-cn1'
  AND contract_agreement_id IS NULL;
```

### `queryNegotiations`
For the sake of simplicity we'll only consider `QuerySpec#limit` and `QuerySpec#offset` for now. Filtering and sorting
will be implemented later.


### `nextForState`
The semantics of this is to select a number of rows and immediately "lease" them, i.e. create an entry in the `lease` table
and update the `lease_id` column in `contract_negotiations:
```sql
-- 1. select 
SELECT * FROM contract_negotiation
WHERE state=200
  AND (lease_id IS NULL OR lease_id IN (SELECT lease_id FROM lease WHERE (NOW > (leased_at + lease.lease_duration))))
LIMIT 5;

-- 2. insert lease
INSERT INTO lease (lease_id, leased_by, leased_at, lease_duration)
VALUES ('lease-1', 'yomama', NOW, default);

-- 3. update contract_negotiation
UPDATE contract_negotiation
SET lease_id='lease-1'
WHERE id IN ('test-cn1');
```