# Refactorings for Architectural Alignment - Meeting Protocol

1. Flatten `/request/handler` to `/handler`. Retain `/description` subpackage (seemingly minor but this created a lot of confusion).
2. Rename `/http` to `/message` (minor, but easy to do)
3. Remove ConfigurationProvider and its implementations; replace with fail-fast POJOs that are specific to each requiring service. Remove this interface from `ids.spi`.
4. Remove all other classes in `ids.spi.version`. There should be one version type (not separate inbound/outbound) and that should be explicitly set as a code constant, not obtained
   from an artifact assumed to be on the classpath or in a manifest.
5. Remove `RejectionMultipartRequestHandler` and `RejectionMessageFactory`
6. Remove `DescriptionRequestMessageHandlerRegistry` and handle dispatching to Sub Handlers directly in `DescriptionRequestHandler`.
7. Remove `ResourceCatalogFactory` and place any logic in the `ConnectorDescriptionService`. If this logic needs to be factored out in the future, create another class and pass it
   to the ctor.
8. Ditto for `DescriptionResponseMessageFactory`.
9. Ditto for `BaseConnectorFactory` and remove one line methods.

## Discussion Points

1. IdsId type is complicated and seems to do multiple things.

   a. IdsId parse(String urn) - Can that be moved to the Description sub handler for dispatching? Also, just take the URI, get the scheme-specific part and tokenize it. Don't
   convert to string first.

   b. The enum type does not appear to offer any value if the `DescriptionRequestHandler` handles dispatching to sub handlers.  
   c. The id methods should be removed and handled by a `IdsTypeTransformer`.


2. Method `handle(DescriptionRequestMessage descriptionRequestMessage, String payload)` - Why is payload a string, should it be a type (e.g. a materialized type or Void)? - Leave as string fro now


## Other

1. gradle.properties should not be in ids-api-multipart-endpoint-v1 - there is one properties file with the info-model version in it in the root project (easy to do)
2. Create an interface for ConnectorDescriptionService
3. ContractOffer should use Unix Epoch longs instead of ZonedDateTime
4. Security Profile should default to NONE
5. ConnectorDescriptionRequestHandler should not return a null MultipartResponse:

     ```
      if (!isRequestingCurrentConnectorsDescription(descriptionRequestMessage)) {
            return null;
      }
   ```
