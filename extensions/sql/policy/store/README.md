# SQL Policy Store

Provides SQL persistence for policies.

## Prerequisites

Please apply this [schema](../docs/schema.sql) to your SQL database.

## Entity Diagram

![ER Diagram](../docs/er.png)
<!--
```plantuml
@startuml
entity edc_policies {
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
