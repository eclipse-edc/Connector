# Data Plane Control API

This extension provides the APIs of the Data Plane which are used by the Control Plane to delegate the data transfer 
to its Data Plane. APIs are not intended for public use. See also [Data Plane Public API](../data-plane-public-api/README.md).

### Scope

Provider Push and Streaming: once contract is agreed by both participants, the Control Plane of the provider delegates
the data transfer to its Data Plane through the Control API.

## Technical Details

### Dependencies

| Name    | Description                                  |
|:--------|:---------------------------------------------|
| web-spi | Essentially for the Controllers registration |

## Design Principles

This API relies on the `DataPlaneManager` for executing the actual data transfer, see [Data Plane Module](../../../core/data-plane/README.md) for more details.