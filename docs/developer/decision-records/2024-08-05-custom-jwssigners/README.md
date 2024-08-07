# Allowing custom `JWSSigner` implementations

## Decision

To allow custom `JWSSigner` implementations, the `TokenGenerationService` needs to be refactored, in particular the way
how `PrivateKey` objects are handled.

## Rationale

In the current EDC implementation, the private key is always resolved from the vault, and a `JWSSigner` implementation
is instantiated based on the algorithm of the key.

Some High-Security Modules (HSMs) have a feature where the signing of content can be done with a remote call while
the private key material does not leave the HSM itself.

To support this feature in EDC, the `TokenGenerationService` and its implementation, the `JwtGenerationService` must be
refactored to accommodate the custom `JWSSigner`.

## Approach

- refactor the `generate()` method: instead of taking a `Supplier<PrivateKey`, the `TokenGenerationService` will take a
  plain String containing the private key ID.
- a new interface `JwsSignerProvider` is added in its own SPI module `jwt-signer-spi`:
  ```java
  public interface JwsSignerProvider extends Function<String, JWSSigner> {
    // marker interface
  }
  ```
  Note that this will expose Nimbus classes in the SPI module!
- the `JwtGenerationService` takes in its constructor a `JwsSignerProvider`, which is then used upon token generation to
  get a `JWSSigner` for a particular private key ID.
  ```java
  @Override
  public Result<TokenRepresentation> generate(String privateKeyId, @NotNull TokenDecorator... decorators) {
    var tokenSigner = jwsGeneratorFunction.apply(privateKeyId);
    if (tokenSigner == null) {
      return Result.failure("JWSSigner cannot be generated for private key '%s'".formatted(privateKeyId));
    }
    //...
  }
  ```
- The default implementation will simply resolve the private key from
  the vault and create a `JWSSigner` for it:
  ```java
  @Provider(isDefault = true)
  public JwsSignerProvider defaultSignerProvider() {
    return privateKeyId -> {
      var pk = privateKeyResolver.resolvePrivateKey(privateKeyId).orElseThrow(/*exception*/);
      return CryptoConverter.createSignerFor(pk);
    };
  }
  ```