# SQL Policy Store

Provides SQL persistence for policies.

## Prerequisites

Please apply this [schema](docs/schema.sql) to your SQL database.

## Entity Diagram

![ER Diagram](https://plantuml.com/plantuml/png/VT11QWCn38NX_Pn2wq0kK4B8lflUm3YUrucAHpAI1ZAKtht5RQ6XcIpz_WyCEdbaYsMk0oGuLi9OKjFAXU7qFX3jg3_NnECJUHZBH8V3o_Fn-1Nt-ovoAN1Ft_2FUdELxdPKLC9oYOV8KL52BU7Q3EwiSXyssCoz-mOmlV2POrkCIp0s6gu1SJvNf6RQODkKvGBZyFHgEl-INh8xJmAxD-9cZ6mJucRwkNci3V___HXEa76PwoS0)
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
