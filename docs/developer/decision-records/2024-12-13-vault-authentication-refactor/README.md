# Hashicorp Vault authentication refactor

## Decision

Refactor the HashiCorp Vault extension and make it extensible with different methods of authentication.

## Rationale

The current implementation of the authentication in the HashiCorp Vault extension has no way to add new authentication methods.
The full list of possible authentication methods can be found in the [HashiCorp Vault Documentation](https://developer.hashicorp.com/vault/docs/auth)
Relevant examples are:

* [Token auth](https://developer.hashicorp.com/vault/docs/auth/token)
* [Kubernetes auth](https://developer.hashicorp.com/vault/docs/auth/kubernetes)
* [AppRole auth](https://developer.hashicorp.com/vault/docs/auth/approle)

## Approach

The refactor will affect only the `vault-hashicorp` extension.

To allow an extensible authentication for HashiCorp Vault, the implementation will follow the provider pattern with a default implementation.

### Hashicorp Vault Auth Interface

To implement a multitude of different authentication methods, an interface for the Hashicorp Vault authentication is needed.
The sole common point between the authentication methods is the ability to provide a `client_token` that can be used to authenticate with HashiCorp Vault.

```java
public interface HashicorpVaultAuthClientTokenProvider {
    
    // The stored token is returned.
    String vaultToken();
    
}
```

`HashicorpVaultAuthClientTokenProvider` implementations will be used by the `HashicorpVaultClient` to receive the `client_token` for the request authentication.
More on that in the sections [Hashicorp Vault Auth Provision](#Hashicorp-Vault-Auth-Provision) and [HashiCorp Vault Client](#HashiCorp-Vault-Client)

### Haschicorp Vault Auth Extensions

For every authentication method, an implementation of the [Hashicorp Vault Auth interface](#hashicorp-vault-auth-interface) is needed.
Each `HashicorpVaultAuthClientTokenProvider` implementation represents a different authentication method and is packaged inside its own service extension.
In this way, it can easily be added/removed from the runtime and maintained in one place.
Due to the possible differences in the needed configuration of different authentication methods, each Service Extension will need its own configuration values specific to the authentication method.
The exception will be the token authentication, which is already packaged inside the `HashicorpVaultExtension` and used as the default authentication method.

### Hashicorp Vault Auth Provision

The `HashicorpVaultAuthTokenImpl` implementation will serve as default authentication mechanism and will be provided by the `HashicorpVaultExtension`.

```java
@Provider(isDefault = true)
public HashicorpVaultAuthClientTokenProvider HashicorpVaultAuthClientTokenProvider() {
    return new HashicorpVaultAuthTokenImpl();
}
```

`isDefault = true` signifies, that `HashicorpVaultAuthTokenImpl` will be used unless another extension overwrites it by providing its own implementation without the parameter.

The provided implementation of `HashicorpVaultAuthClientTokenProvider` is then injected into the `HashicorpVaultExtension` to be used in the `HashicorpVaultClient` later on.

```java
@Inject
private HashicorpVaultAuthClientTokenProvider hashicorpVaultAuthClientTokenProvider;
```

### HashiCorp Vault Client

Since the `HashicorpVaultClient` contains a lot of authentication logic, it will also go through a refactoring.
The goal of the refactoring, is the removal of the token authentication logic from `HashicorpVaultClient` and to make the authentication logic interchangeable.
An implementation of `HashicorpVaultAuthClientTokenProvider` is passed to `HashicorpVaultClient` during creation by `HashicorpVaultExtension`.
`HashicorpVaultClient` will use the `HashicorpVaultAuthClientTokenProvider` implementation to fetch the `client_token`.
`client_token` will then be used to generate the Headers for the HTTP requests and to authenticate with HashiCorp Vault.

Old `getHeaders()`:

```java
@NotNull
private Headers getHeaders() {
    var headersBuilder = new Headers.Builder().add(VAULT_REQUEST_HEADER, Boolean.toString(true));
    headersBuilder.add(VAULT_TOKEN_HEADER, settings.token());
    return headersBuilder.build();
}
```

New `getHeaders()`:

```java
@NotNull
private Headers getHeaders(String token) {
    var headersBuilder = new Headers.Builder().add(VAULT_REQUEST_HEADER, Boolean.toString(true));
    headersBuilder.add(VAULT_TOKEN_HEADER, token);
    return headersBuilder.build();
}
```

An important change that was made, is the point at which the headers are generated.
Previously, the headers were generated only once since the token was not liable to change.
With the addition of numerous potential authentication mechanisms, it can no longer be guaranteed, that the token never changes during refresh.
An example for this would be the kubernetes authentication method, where short term tokens can be produced depending on the settings.
As such, `headers` are no longer saved as a variable inside the `HashicorpVaultClient` and `getHeaders()` is called instead to always fetch the newest token.

### HashiCorp Vault Extension Refactor

The token based authentication logic is refactored and moved into dedicated classes in the newly created `auth` package.
This includes the token refresh functionality and will lead to a refactoring of the following classes:

`HashicorpVaultClient.java`: `isTokenRenewable()` and `renewToken()` and their private methods will be moved to a new dedicated client class.

`HashicorpVaultTokenRenewTask.java`: is moved to the `auth` package in its entirety.

The respective tests covering the moved functionality are also moved accordingly.


### HashiCorp Vault Health Extension

The `get()` method of `HashicorpVaultHealthCheck` is currently using a merge of the response from the Hashicorp Vault Health API and the ability to renew the given token.
After the refactor, the token renewal lookup functionality  is no longer guaranteed to exist inside the `HashicorpVaultExtension`.
As such the `get()` method is adjusted and will only return the response from the Hashicorp Vault Health API.

## Further Considerations

This Decision Record only paves the way to add additional authentication methods later on.
As such, possible authentication methods that can be added in the future are not discussed in more detail here, but will require their own Decision Records containing the outline of the implementation and testing.
