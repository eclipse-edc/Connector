# SQL Asset

Provides SQL persistence for assets.

## Prerequisites

Please apply this [schema](docs/schema.sql) to your SQL database.

## Entity Diagram

![ER Diagram](https://www.plantuml.com/plantuml/png/ZP3D2i8m48JlUOez2ta1AQLtBxv1MDn58crQibiXDBwxGQfKhJ-tm3SpcPr65AEENMiugDS4J0U78gmm6O0DtDxEqnP4emz7gAhzhguBizPSp9lD4IeYKMIHNn653R4VEAfdMT2JzE7R5xCf_P-VNC2Exu9dSiPs_80q3KiortaibBErEQ_V_YBhfvN-fk50PVih)
<!--
```plantuml
@startuml
entity edc_asset {
  * asset_id_fk: string <<PK>>
  --
}

entity edc_asset_dataaddress {
  * asset_id_fk: string <<PK>>
  * properties: string <<json>>
  --
}

entity edc_asset_property {
  * asset_id_fk: string <<PK>>
  * property_name: string
  * property_value: string
  * property_type: string
  --
}

edc_asset ||--|| edc_asset_dataaddress
edc_asset ||--o{ edc_asset_property
@enduml
```
-->

## Configuration

| Key | Description | Mandatory | 
|:---|:---|---|
| edc.datasource.asset.name | Datasource used by this extension | X |
