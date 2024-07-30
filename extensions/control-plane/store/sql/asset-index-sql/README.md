# SQL Asset

Provides SQL persistence for assets.

## Prerequisites

Please apply this [schema](src/main/resources/asset-index-schema.sql) to your SQL database.

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

| Key                       | Description                       | Mandatory | 
|:--------------------------|:----------------------------------|-----------|
| edc.datasource.asset.name | Datasource used by this extension | X         |

## Migrate from 0.3.1 to 0.3.2

This table structure has been changed, from 3 tables (one for the asset, one for properties and one for data address) to
a single table with json fields.
To migrate an existing database, please first add the 3 new columns in the `edc_asset` table:

```sql
ALTER TABLE edc_asset
    ADD COLUMN properties         json DEFAULT '{}',
    ADD COLUMN private_properties json DEFAULT '{}',
    ADD COLUMN data_address       json DEFAULT '{}';
```

Then migrate the existing data in the new format:

```sql
UPDATE edc_asset
SET properties=(SELECT JSON_OBJECT_AGG(property_name, property_value)
                FROM edc_asset_property
                WHERE asset_id_fk = edc_asset.asset_id
                  AND property_is_private = FALSE),
    private_properties=(SELECT JSON_OBJECT_AGG(property_name, property_value)
                        FROM edc_asset_property
                        WHERE asset_id_fk = edc_asset.asset_id
                          AND property_is_private = TRUE),
    data_address=(SELECT properties FROM edc_asset_dataaddress WHERE asset_id_fk = edc_asset.asset_id);
```

After the migration, the tables `edc_asset_dataaddress` and `edc_asset_property` can be deleted.
