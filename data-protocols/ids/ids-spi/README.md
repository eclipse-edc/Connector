**Please note**

### Work in progress

All content reflects the current state of discussion, not final decisions.

---

# IDS SPI

## Configuration

| Key |  Description |
|:---|:---|
| edc.ids.id | The ID of the connector (e.g. urn:connector:edc-connector-24) |
| edc.ids.title | Localized name of the connector |
| edc.ids.description | Description of the connector in natural language |
| edc.ids.maintainer | Participant responsible for the technical maintenance of the infrastructure |
| edc.ids.curator |  Participant responsible for the correctness of the offered content |
| edc.ids.endpoint | Default endpoint for basic infrastructure interactions |
| edc.ids.security.profile |  Supported IDS security profile |
| edc.ids.catalog.id | The ID of the data catalog (e.g. urn:catalog:ids-catalog-45) |

## IDS Asset Property

| Asset Property Key | IDS Target |  Description |
|:---|:---|:---|
| ids:fileName | Artifact.FileName | Name of the Artifact file |
| ids:byteSize | Artifact.ByteSize | Size of the Artifact in bytes |
| ids:fileExtension | Representation.MediaType.FileExtension | Suffix of a file name, indicating the nature and intended processing of the file |