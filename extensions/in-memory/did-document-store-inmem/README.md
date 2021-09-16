This module implements the `DidDocumentStore` interface, which basically acts as a local in-memory cache for DidDocuments. The `RegistrationService` will periodically scan the ION network and
cache all DidDocuments, that are of a certain type, in its internal cache.
