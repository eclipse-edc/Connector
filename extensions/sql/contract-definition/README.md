# SQL Contract Definition

Provides SQL persistence for contract definitions.

## Prerequisites

Please apply this [schema](docs/schema.sql) to your SQL database.

## Entity Diagram

![ER Diagram](https://www.plantuml.com/plantuml/png/VSz12i9038NX_PmYQw4Na5AwT-CDXY4J9J8c9ObW4U_kuAgWTFq-7lopHx5ut5iY2OuLg696bR22aIjTM3XOnU6L00xmPyfSZ-1XB5SOn_DfcheSXYylYEIU5ssCwqRTNAMxxUCkScg4ePRfkHYviyelVAUPfFQM3m00)
<!--
```plantuml
@startuml
entity edc_contract_definitions {
  * contract_definition_id: string <<PK>>
  --
  * access_policy: string <<json>>
  * contract_policy: string <<json>>
  * selector_expression: string <<json>>
}
@enduml
```
-->

## Configuration

| Key | Description | Mandatory | 
|:---|:---|---|
| edc.datasource.contractdefinition.name | Datasource used by this extension | X |
