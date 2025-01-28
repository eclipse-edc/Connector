# Hashicorp Vault authentication refactor

## Decision

We will refactor the HashiCorp Vault extension and make it extensible with different methods of authentication.

## Rationale

The current implementation of the authentication in the HashiCorp Vault extension has no way to customize/exchange authentication methods.
The full list of possible authentication methods can be found in the [HashiCorp Vault Documentation](https://developer.hashicorp.com/vault/docs/auth)
Relevant examples are:

* [Token auth](https://developer.hashicorp.com/vault/docs/auth/token)
* [Kubernetes auth](https://developer.hashicorp.com/vault/docs/auth/kubernetes)
* [AppRole auth](https://developer.hashicorp.com/vault/docs/auth/approle)

## Approach

The refactor will affect only the `vault-hashicorp` module.
To allow an extensible authentication for HashiCorp Vault, the implementation will follow the provider pattern with a default implementation.

### Hashicorp Vault Auth Interface

To support multiple authentication methods, an interface is defined for the HashiCorp Vault authentication.
The common point among all methods is the ability to provide a `client_token` used for authentication.

```java
public interface HashicorpVaultAuthClientTokenProvider {
    
    // The stored token is returned.
    String vaultToken();
    
}
```

### Hashicorp Vault Token Auth Method Implementation

The default implementation of `HashicorpVaultAuthClientTokenProvider` provided by the `HashicorpVaultExtension` will use the [Token auth method](https://developer.hashicorp.com/vault/docs/auth/token).

```java
@Provider(isDefault = true)
public HashicorpVaultAuthClientTokenProvider hashicorpVaultAuthClientTokenProvider() {
    return new HashicorpVaultTokenAuthMethodImpl();
}
```

`HashicorpVaultTokenAuthMethodImpl` is then used by services in the `vault-hashicorp` module for authentication with the HashiCorp Vault instance. 
These services are:

* Secure key-value store `HashicorpVault`
* Signing service `HashicorpVaultSignatureService`
* Health check service `HashicorpVaultHealthService`

`HashicorpVault` and `HashicorpVaultSignatureService` will only need small changes for authentication. 
They will use the `client_token` from the `HashicorpVaultTokenAuthMethodImpl` instead of directly fetching the token from the settings.

### HashiCorp Vault Health Service

An implementation of `HashicorpVaultAuthClientTokenProvider` is passed to `HashicorpVaultHealthService` during creation by `HashicorpVaultExtension`.
`HashicorpVaultHealthService` will use the `HashicorpVaultAuthClientTokenProvider` implementation to fetch the `client_token`.
`client_token` will then be used to generate the Headers for the HTTP requests and to authenticate with HashiCorp Vault.

```java
@NotNull
private Headers getHeaders(String token) {
    var headersBuilder = new Headers.Builder().add(VAULT_REQUEST_HEADER, Boolean.toString(true));
    headersBuilder.add(VAULT_TOKEN_HEADER, token);
    return headersBuilder.build();
}
```

Previously, the headers were generated only once since the token was not liable to change.
With the addition of numerous potential authentication mechanisms, it can no longer be guaranteed, that the token never changes during refresh.
An example for this would be the kubernetes authentication method, where short term tokens can be produced depending on the settings.
As such, `headers` are no longer saved as a variable inside the `HashicorpVaultClient` and `getHeaders()` is called instead to always fetch the newest token.

### HashiCorp Vault Health Extension

The `get()` method of `HashicorpVaultHealthCheck` is currently using a merge of the response from the Hashicorp Vault Health API and the ability to renew the given token.
After the refactor, the token renewal lookup functionality  is no longer guaranteed to exist inside the `HashicorpVaultExtension`.
As such the `get()` method is adjusted and will only return the response from the Hashicorp Vault Health API.

## Further Considerations

This Decision Record only paves the way to add additional authentication methods later on.
As such, possible authentication methods that can be added in the future are not discussed in more detail here, but will require their own Decision Records containing the outline of the implementation and testing.
