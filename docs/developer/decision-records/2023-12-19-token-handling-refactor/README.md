## Decision

Token generation and verification will be refactored to remove complexity, reduce technical debt, and increase the reusability of core token services.

## Rationale

Token generation and verification have evolved organically over time and contain a number of inconsistencies and unnecessary complexity. This Decision Record details how these code paths will be refactored to provide more streamlined, concise, and consistent token handling services.

## Impact

The refactoring will impact core control plane services as described below.

## Main Refactoring      

The refactoring will be done in seven steps.

### Step 1: Simplify `PrivateKeyResolver` and remove `KeyPairFactory`

The `PrivateKeyResolver` interface contains methods for adding a key parser. This will be decoupled by introducing a  `KeyParserRegistry` that a `PrivateKeyResolver` implementation can use:

```java
@FunctionalInterface
interface KeyParser<T> {

 T parse(String encoded);
 
}

interface KeyParserRegistry {

   <T> void register(Class<T> type, KeyParser<T> parser);

   <T> T parse(Class<T> type, String value);

}
```

 `PrivateKeyResolver` will then be simplified to:

```
@FunctionalInterface
@ExtensionPoint
public interface PrivateKeyResolver {

    @Nullable PrivateKey resolvePrivateKey(String id);

}
```

The `KeyPairFactory` will be removed and replaced by individual resolvers or default creation.
### Step 2: Introduce dynamic key resolution and security contexts

Currently, private key resolution is performed statically at runtime boot. This precludes key rotation without a runtime restart. In addition, static resolution potentially introduces security vulnerabilities by maintaining in-memory references to private key material. To enable key rotation a reduce the chance of key leakage, all private keys will be resolved on demand from secure storage. This will greatly simplify the existing `TokenGenerationService` by making it possible to have one service instance per runtime. Moreover, it will break the implementation coupling EDC identity modules have on `TokenGenerationServiceImpl` and the `jwt-core` module. 

The design of the current EDC token handling is limited by its inability to accommodate the requirements of different token types easily. Specifically, token generation and validation depend on the security context in which an operation is performed. For example, OAuth2 and self-issued token handling have different generation and validation procedures. `TokenGenerationService` and `TokenValidationService` will be refactored to introduce the concept of token types.

### TokenGenerationService

`TokenGenerationService` will be changed to remove the private key parameter from its constructor and instead have a `Supplier<PrivateKey>` passed to it (which may wrap a `PrivateKeyResolver`):

```java
public interface TokenGenerationService {

   Result<TokenRepresentation> generate(Supplier<PrivateKey> keySupplier, JwtDecorator... decorators);

}
```

### TokenValidationService

Many rules that apply to token validation depend on the token type. For example, OAuth token validation requires different rules than self-issued token validation. Validation rules will be associated with a token type and resolved per validation operation (there will also be default rules that apply to all types).  The `TokenValidationService` and `TokenValidationRulesRegistry` will be refactored to accommodate these changes.

`TokenValidationService` will have a `PublicKeyResolver` and `TokenValidationRules` passed when performing validation, rather than `TokenValidationServiceImpl` taking a reference to a `PublicKeyResolver` and the `TokenValidationRulesRegistry` in its constructor:

```java
public interface TokenValidationService {

  Result<ClaimToken> validate(TokenRepresentation representation, PublicKeyResolver resolver, TokenValidationRule... rules);

  Result<ClaimToken> validate(@NotNull String token, PublicKeyResolver resolver, TokenValidationRule... rules);
  
}
```

The client that invokes the `TokenValidationService` will be responsible for providing the public key resolver and assembling the rules for the current security context. Public key resolvers will implement different strategies for returning the key:
- IATP - The implementation will resolve key material from a DID
- OAuth - The implementation will use the `IdentityProviderKeyResolver`
- Consumer pull data planes - May resolve from config, a vault, or some other form of secure storage

> Note that the token type cannot be passed to the `TokenValidationService` in place of the array of `TokenValidationRules` since some rules depend on the current security context. For example, audience validation requires access to an audience value tied to the current runtime request.  These rules will typically be lambdas passed to the service.

### `JwtDecoratorRegistry` and `TokenValidationRulesRegistry` 

The `JwtDecoratorRegistry` and `TokenValidationRulesRegistry` specializations (subclasses) will be removed and replaced with the concept of a token type, e.g. OAuth2:

```java
public interface JwtDecoratorRegistry {

  void register(String type, JwtDecorator decorator);
  
  void unregister(String type, JwtDecorator decorator);

  List<JwtDecorator> decoratorsFor(String... types);
  
}
```

```java
public interface TokenValidationRulesRegistry {

  void addRule(String type, TokenValidationRule rule);

  List<TokenValidationRule> rulesFor(String... types);
  
}
```

The client calling the `TokenGenerationService` or `TokenValidationService` will consult the registry for applicable decorators and rules to pass.

> The DAPs extension will reference an OAuth token type from the OAuth SPI to add its customizations to the registries.

>  `JwtDecorator` will be renamed to `TokenDecorator` and converted to a functional interface with default methods. All implementations will create either headers or claims.
> 

### Step 3: Remove `TokenDecorator` and `CredentialsRequestAdditionalParametersProvider`

Currently, multiple extension points are used to determine the contents of a token, including `TokenDecorator`, `CredentialsRequestAdditionalParametersProvider`, and `TokenDecorator`. The refactoring will converge on `TokenDecorator` as a single extension point.

`CredentialsRequestAdditionalParametersProvider` will be removed.

The `TokenDecorator` extension point is a singleton service invoked at the remote dispatch layer (`DspHttpRemoteMessageDispatcherImpl` ). There is currently one implementation in the DAPs extension. This should be refactored to use a `JwtDecorator`.

To migrate the `TokenDecorator` functionality to `JwtDecorators`, the following changes will be made. 

First, `TokenParameters` should treat scope as a normal parameter (rename `additional` to `parameters`):

```java
    private final Map<String, Object> parameters = new HashMap<>();
    
    private String audience;

    private TokenParameters() {
    }

    public String getAudience() {
        return audience;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

```

The `scope` and `grant_type` specializations will then be removed from `Oauth2CredentialsRequest`.

`Oauth2ServiceImpl` can then be simplified: 

```java

    @NotNull
    private Oauth2CredentialsRequest createRequest(TokenParameters parameters, String assertion) {
        return PrivateKeyOauth2CredentialsRequest.Builder.newInstance()
                .url(configuration.getTokenUrl())
                .clientAssertion(assertion)
                .build();
    }
```

> Note that the call to `Oauth2CredentialsRequest.scope()` and the use of `CredentialsRequestAdditionalParametersProvider` is removed in the above example.


The DAPS `DapsTokenDecorator` implementation can then be converted to a `JwtDecorator` and registered for OAuth token types with the `JwtDecoratorRegistry`. 

### Step 4: Remove `JwtUtils`

Remove `JwtUtils` as `TokenGenerationService` can replace it. This is only used in the `DecentralizedIdentityService`, so it can be deprecated and removed when the latter is removed. 

### Step 5: Audience Handling

#### Egress

The DSP layer incorrectly handles the `audience` for token creation. In `DspHttpRemoteMessageDispatcherImpl`:

```java 
var tokenParameters = 
  tokenParametersBuilder.audience(message.getCounterPartyAddress()).build();
```

The audience must be determined by the `IdentityService` implementation. For example, the audience may be mapped from the counterparty id to a DID (`IdentityAndTrustService`) or the counterparty URL (`Oauth2ServiceImpl`). To accommodate these differences, `RemoteMessage` will have a `counterPartyId()` method added:

```java
String getCounterPartyId();
```

Counterparty id can be obtained from the `ContractNegotiation` or `TransferProcess`.

`TokenParameters` will be adjusted to:

- Remove the `audience` property
- Add a `counterPartyId` property
- Add a `counterPartyAddress` property

These will be used by the `IdentityService` implementation to determine the `audience`.

#### Ingress

`IdentityService` implementations are responsible for validating the audience for tokens associated with incoming requests. This will not change. For example, the OAuth2 extension will continue to supply the `Oauth2AudienceValidationRule` to the rules registry for OAuth token types. The  `IdentityAndTrustService` implementation will continue to use the participant id from the `ServiceExtensionContext` to map to a DID.

## Miscellaneous Refactoring

###  `IdentityProviderKeyResolver`

`IdentityProviderKeyResolver` will be refactored to use a builder and remove references to `IdentityProviderKeyResolverConfiguration`.

### `TransferDataPlaneCoreExtension`
 
  The injected `privateKeyResolver` field can be removed as it is unused.
