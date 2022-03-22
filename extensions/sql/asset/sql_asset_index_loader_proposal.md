## ER Diagramm

![ER Diagram](http://www.plantuml.com/plantuml/png/XP513e8m44NtFSMiJNe1GWWhDnwY6Ud4b40XdS58XBjRnI346dVD_DVVtpyb2mOPsaQH5oSZaAqCCfF0NG4Su7KspcR04fo_nA7MQbPVlB4eYDO6Olvn5_ByU2gAnU89zA7hAOWZpa3e9X6ekVCHIJtmfiU_xglnQ3osj8yE7t4PcoismmfjzUvSKoLxVHznBMkQj4vtiO7c22N-eqhoDgxW0G00)

## Table schema

```sql

-- table: edc_asset
CREATE TABLE IF NOT EXISTS edc_asset (
    asset_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (asset_id)
);


-- table: edc_asset_dataaddress
CREATE TABLE IF NOT EXISTS edc_asset_dataaddress (
    asset_id VARCHAR(255) NOT NULL,
    properties VARCHAR(MAX) NOT NULL,
    PRIMARY KEY (asset_id),
    FOREIGN KEY (asset_id) REFERENCES edc_asset (asset_id) ON DELETE CASCADE
);

COMMENT ON COLUMN edc_asset_dataaddress.properties is 'DataAddress properties serialized as JSON';


-- table: edc_asset_property
CREATE TABLE IF NOT EXISTS edc_asset_property (
    asset_id VARCHAR(255) NOT NULL,
    property_name VARCHAR(255) NOT NULL,
    property_string_value VARCHAR(MAX) DEFAULT NULL,
    property_object_value VARCHAR(MAX) DEFAULT NULL,
    PRIMARY KEY (asset_id, property_name),
    FOREIGN KEY (asset_id) REFERENCES edc_asset (asset_id) ON DELETE CASCADE,
    CONSTRAINT con_edc_asset_property_single_value CHECK (
        (property_string_value IS NOT NULL AND property_object_value IS NULL)
     OR (property_string_value IS NULL AND property_object_value IS NOT NULL)
     OR (property_string_value IS NULL AND property_object_value IS NULL))
);

COMMENT ON COLUMN edc_asset_property.property_name IS 
    'Asset property key';

COMMENT ON COLUMN edc_asset_property.property_string_value IS  
    'Asset property string value if property of type string';

COMMENT ON COLUMN edc_asset_property.property_object_value IS 
    'Asset property object value serialized as JSON if not of type string';

COMMENT ON CONSTRAINT con_edc_asset_property_single_value IS  
    'All value fields are NULL, otherwise exclusively one of the value columns is non-NULLable';

CREATE INDEX IF NOT EXISTS idx_edc_asset_property_string_value 
  ON edc_asset_property(property_name, property_string_value);
```

## DML - AssetLoader
#### AssetLoader: `void accept(Asset asset, DataAddress dataAddress)`

Given example Asset data

<table>
  <thead>
    <tr>
      <td><b>id</b></td>
      <td><b>asset:prop:name</b></td>
      <td><b>asset:prop:description</b></td>
      <td><b>asset:prop:version</b></td>
      <td><b>asset:prop:contenttype</b></td>
      <td><b>data-address</b></td>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>784cdfde-a50c-11ec-b909-0242ac120002</td>
      <td>Max Power</td>
      <td>Homer to the Max</td>
      <td><i>NULL</i></td>
      <td><i>NULL</i></td>
      <td>
        <code>
          {
            "uri": "https://projects.eclipse.org/proposals/eclipse-dataspace-connector"
          }
        </code>
      </td>
    </tr>
    <tr>
      <td>80b69de0-a50c-11ec-b909-0242ac120002</td>
      <td>Seneca</td>
      <td>Seneca the Younger</td>
      <td><i>v2</i></td>
      <td><i>application/pdf</i></td>
      <td>
        <code>
          {
            "uri": "https://archive.org/download/adlucilium02sene/adlucilium02sene.pdf"
          }
        </code>
      </td>
    </tr>
  </tbody>
</table>

would translate into following DML:

```sql
BEGIN;

INSERT INTO edc_asset (asset_id) VALUES ("784cdfde-a50c-11ec-b909-0242ac120002");
INSERT INTO edc_asset_property (asset_id, property_name, property_string_value) VALUES ("784cdfde-a50c-11ec-b909-0242ac120002", "asset:prop:name", "Max Power");
INSERT INTO edc_asset_property (asset_id, property_name, property_string_value) VALUES ("784cdfde-a50c-11ec-b909-0242ac120002", "asset:prop:description", "Homer to the Max");
INSERT INTO edc_asset_dataaddress (asset_id, properties) VALUES ("784cdfde-a50c-11ec-b909-0242ac120002", "{ \"uri\": \"https://projects.eclipse.org/proposals/eclipse-dataspace-connector\" }")

INSERT INTO edc_asset (asset_id) VALUES ("80b69de0-a50c-11ec-b909-0242ac120002");
INSERT INTO edc_asset_property (asset_id, property_name, property_string_value) VALUES ("80b69de0-a50c-11ec-b909-0242ac120002", "asset:prop:name", "Seneca");
INSERT INTO edc_asset_property (asset_id, property_name, property_string_value) VALUES ("80b69de0-a50c-11ec-b909-0242ac120002", "asset:prop:description", "Seneca the Younger");
INSERT INTO edc_asset_property (asset_id, property_name, property_string_value) VALUES ("80b69de0-a50c-11ec-b909-0242ac120002", "asset:prop:version", "v2");
INSERT INTO edc_asset_property (asset_id, property_name, property_string_value) VALUES ("80b69de0-a50c-11ec-b909-0242ac120002", "asset:prop:contenttype", "application/pdf");
INSERT INTO edc_asset_dataaddress (asset_id, properties) VALUES ("80b69de0-a50c-11ec-b909-0242ac120002", "{ \"uri\": \"https://archive.org/download/adlucilium02sene/adlucilium02sene.pdf\" }")


COMMIT;
```

#### AssetLoader: `Asset deleteById(String assetId)`
Soft deletion has been postponed. For noe, deletion will work as follows.

```sql
DELETE FROM edc_asset WHERE asset_id = "784cdfde-a50c-11ec-b909-0242ac120002";
```


## DQL - AssetIndex

#### AssetIndex: `Stream<Asset> queryAssets(QuerySpec querySpec)`

Given an example Asset data set

<table>
  <thead>
    <tr>
      <td><b>id</b></td>
      <td><b>asset:prop:name</b></td>
      <td><b>asset:prop:description</b></td>
      <td><b>asset:prop:version</b></td>
      <td><b>asset:prop:contenttype</b></td>
      <td><b>data-address</b></td>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>784cdfde-a50c-11ec-b909-0242ac120002</td>
      <td>Max Power</td>
      <td>Homer to the Max</td>
      <td><i>NULL</i></td>
      <td><i>NULL</i></td>
      <td>
        <code>
          {
            "uri": "https://projects.eclipse.org/proposals/eclipse-dataspace-connector"
          }
        </code>
      </td>
    </tr>
  </tbody>
</table>

to be queried for the first 50 entries by an asset having property **asset:prop:name** to be exactly `Max Power` and **asset:prop:description** to be exactly `Homer to the Max`
would be expressed via SQL like

```sql
SELECT DISTINCT (a.asset_id) FROM edc_asset AS a
WHERE EXISTS (
    SELECT 1 FROM edc_asset_property AS eap 
      WHERE eap.asset_id = a.asset_id AND eap.property_name = 'asset:prop:name' AND eap.property_string_value = 'Max Power')
  AND EXISTS (
    SELECT 1 FROM edc_asset_property AS eap 
      WHERE eap.asset_id = a.asset_id AND eap.property_name = 'asset:prop:description' AND eap.property_string_value = 'Homer to the Max')
ORDER BY a.asset_id DESC
LIMIT 50 OFFSET 0;
```

#### Limitations

Sorting - **except by asset_id** - is not possible.
