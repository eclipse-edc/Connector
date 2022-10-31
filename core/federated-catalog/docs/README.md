## The catalog extensions

This extension includes the `catalog.org.eclipse.edc.connector.controlplane.spi.CatalogService` interface, which serves as a generic convenience feature to issue a catalog query to any EDC-compliant connector. It hides all the technical details 
like IDS-messages and the like from the user. 

All that's required is a `connectorName` and a `connectorId` of the connector, who's catalog is to be queried.

Implementors of connectors do not _need_ the `catalog.org.eclipse.edc.connector.controlplane.spi.CatalogService` but it makes things a lot easier.
