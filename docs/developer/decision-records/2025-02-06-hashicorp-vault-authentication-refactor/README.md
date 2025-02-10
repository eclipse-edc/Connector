# HashiCorp Vault authentication refactor

## Decision

We will refactor the HashiCorp Vault implementation to support the usage of different methods of authentication.

## Rationale

The HashiCorp Vault supports multiple different authentication methods, but the current implementation supports
only one and provides no way to customize or exchange authentication methods. The full list of supported
methods can be found in the [HashiCorp Vault Documentation](https://developer.hashicorp.com/vault/docs/auth). Some examples are:

* [Token auth method](https://developer.hashicorp.com/vault/docs/auth/token) (already supported)
* [Kubernetes auth method](https://developer.hashicorp.com/vault/docs/auth/kubernetes)
* [AppRole auth method](https://developer.hashicorp.com/vault/docs/auth/approle)

## Approach

The refactoring will be limited to the `vault-hashicorp` module.

The common denominator among all authentication methods is the ability to provide a `client_token` for
authentication against the vault. Therefore, obtaining this token will be moved to an interface. 

```java
@ExtensionPoint
public interface HashicorpVaultTokenProvider {
    String vaultToken();
}
```

This interface will be the extension point for downstream projects to introduce different authentication methods by
providing a respective implementation of this interface to the runtime context.
The [token auth method](https://developer.hashicorp.com/vault/docs/auth/token) is already supported by the current implementation and will remain the default
implementation provided through the `HashicorpVaultExtension`.
The respective code will therefore be moved to a class implementing `HashicorpVaultTokenProvider`.

### Services accessing the vault

Any service that needs to access the vault will be refactored to use the `HashicorpVaultTokenProvider` for obtaining
the token. Namely, affected services are:

* `HashicorpVault` (secure key-value store)
* `HashicorpVaultSignatureService` (signing service)
* `HashicorpVaultHealthService` (health check service)

All of these services currently get the token from the `HashicorpVaultSettings`, which will be replaced by a call to
the `HashicorpVaultTokenProvider`.

### HashicorpVaultHealthService

In addition, the `HashicorpVaultHealthService` currently stores the headers for all requests to the vault in a
class variable, which is populated once upon service instantiation. As the token, and thus the headers,
may not be static for all authentication methods, the variable will be removed. Instead, the token will be fetched from
the `HashicorpVaultTokenProvider` and the headers will be generated for every request. The token will therefore be
added as a parameter to the `getHeaders` method.

```java
@NotNull
private Headers getHeaders(String token) {
    var headersBuilder = new Headers.Builder().add(VAULT_REQUEST_HEADER, Boolean.toString(true));
    headersBuilder.add(VAULT_TOKEN_HEADER, token);
    return headersBuilder.build();
}
```

The `HashicorpVaultHealthService` also provides methods for checking whether a token can be renewed as well as for
renewing the token. As these features are not available for all authentication methods, this functionality should be
moved to a different class. This part of the refactoring is already planned anyway for better separation of concerns.

### HashicorpVaultHealthCheck

The `get()` method of `HashicorpVaultHealthCheck` currently returns a merged result, using the response from the
health API and the response to whether the current token can be renewed. After the refactor, it is no longer
guaranteed that token renewal is supported. The second check will therefore be removed from the method, which
then simply returns the result of the health check.

## Further Considerations

This decision record only paves the way to add additional authentication methods in the future. The actual support and
implementation of any additional authentication methods is therefore out of scope of this decision record.
