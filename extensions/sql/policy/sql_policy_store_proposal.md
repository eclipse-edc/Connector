<!-- Copyright header

Copyright (c) 2022 Daimler TSS GmbH

This program and the accompanying materials are made available under the
terms of the Apache License, Version 2.0 which is available at
https://www.apache.org/licenses/LICENSE-2.0

  SPDX-License-Identifier: Apache-2.0

Contributors:
  Daimler TSS GmbH - Initial Design Proposal

-->

## ER diagram

![ER Diagram](https://www.plantuml.com/plantuml/png/TP11oeD034RtdcBMFtY17n7ttVG6WpgfVaKpGnAXKdht3a9Xe7OJo7jl4TAfzMBRkbJ41jiTXT6dk604gNjMplsvOzqzmhzJ4_3ackPwQTiiwpgFHB925P6rAEuQ-MYIn1a3x9SFcu5E3NWcWQc_ILm_GUQzAYOc-KPK8Ejb8Yl1dafF3oDM30lv936Hd9_ngZhYa3_q1W00)

## Table schema

See [schema](docs/schema.sql).

## Methods to implement
The following proposal is based on the interface on this branch [branch](https://github.com/agera-edc/DataSpaceConnector/tree/feature/803-policy-store).

#### PolicyStore: `Policy findById(String policyId)`
Retrieve a single policy via the following query, using the passed policyId.
```sql
SELECT * FROM edc_policies WHERE policy_id=policyId;
```
Return the policy if an exact match is found, otherwise null.

#### PolicyStore: `Stream<Policy> findAll(QuerySpec spec)`
Retrieve all policies based on a passed QuerySpec.
Initially only the Limit and Offset parameters of QuerySpec will be supported.
```sql
SELECT * FROM edc_policies LIMIT limit OFFSET offset;
```
Filtering by non-json parameters will be added at a later date. Filtering based on json parameters is not intended.

#### PolicyStore: `void save(Policy policy)`
Save a single policy. Update if it already exists. Deserialize all non-string parameters and pass them into an appropriate query.
```sql
INSERT INTO edc_policies (
    policy_id,
    permissions,
    prohibitions,
    duties,
    extensible_properties,
    inherits_from,
    assigner,
    assignee,
    target,
    policy_type,
) VALUES (
    policy.id,
    policy.permissions,
    policy.prohibitions,
    policy.duties,
    policy.extensibleProperties,
    policy.inheritsFrom,
    policy.assigner,
    policy.assignee,
    policy.target,
    policy.type,
);
```

```sql
UPDATE edc_policies SET (
    permissions=policy.permissions,
    prohibitions=policy.prohibitions,
    duties=policy.duties,
    extensible_properties=policy.extensibleProperties,
    inherits_from=policy.inheritsFrom,
    assigner=policy.assigner,
    assignee=policy.assignee,
    target=policy.target,
    policy_type=policy.type,
) WHERE policy_id=policy.id;
```

#### PolicyStore: `Policy delete(String policyId)`
Find a policy by ID.
```sql
SELECT * FROM edc_policies WHERE policy_id=policyId;
```
If it exists, delete it from the DB and then return it as a policy object.

```sql
DELETE FROM edc_policies WHERE policy_id=policyId;
```

Return null if it doesn't exist.