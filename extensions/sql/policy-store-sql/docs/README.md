# SQL Policy Store

Provides SQL persistence for policies.

Note that the SQL statements (DDL) are specific to and only tested with PostgreSQL. Using it with other RDBMS may work
but might have unexpected side effects!

## Prerequisites

Please apply this [schema](schema.sql) to your SQL database.

## Entity Diagram

![ER Diagram](er.png)
<!--
```plantuml
@startuml
entity edc_policydefinitions {
  * policy_id: string <<PK>>
  --
  * access_policy: string <<json>>
  * contract_policy: string <<json>>
  * selector_expression: string <<json>>
  * permissions: string <<json>>
  * prohibitions: string <<json>>
  * duties: string <<json>>
  * extensible_properties: string <<json>>
  * inherits_from: string
  * assigner: string
  * assignee: string
  * target: string
  * policy_type: string <<json>>
}
@enduml
```
-->

## Configuration

| Key                        | Description | Mandatory | 
|:---------------------------|:---|---|
| edc.datasource.policy.name | Datasource used by this extension | X |
