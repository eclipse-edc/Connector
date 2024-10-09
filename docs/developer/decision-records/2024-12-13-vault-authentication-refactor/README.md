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

It goes against the EDC extensibility model and needs to be remedied.
Additionally, extracting the current implementation of the token authentication into its own extension will improve readability and maintainability of the code.

## Approach

The refactor will affect only the `vault-hashicorp` extension.
It will create one additional extension called `vault-hashicorp-auth-tokenbased` that contains the token authentication and token renewal functionality.

To allow an extensible authentication for HashiCorp Vault, the implementation will follow the registry pattern.

### Hashicorp Vault Auth SPI

For the proper organisation of the interfaces needed for the refactoring, a new module named `hashicorp-vault-auth-spi` is introduced in the `spi` directory.

It will contain the following interfaces:

* [Hashicorp Vault Auth Interface](#hashicorp-vault-auth-interface)
* [Hashicorp Vault Auth Registry Interface](#hashicorp-vault-auth-registry)

### Hashicorp Vault Auth Interface

To implement a multitude of different authentication methods, an interface for the Hashicorp Vault authentication is needed.
The sole common point between the authentication methods is the ability to provide a `client_token` that can be used to authenticate with HashiCorp Vault.

```java
public interface HashicorpVaultAuth {
    
    // The stored token is returned.
    String vaultToken();
    
}
```

`HashicorpVaultAuth` implementations will be registered in `HashicorpVaultAuthRegistry` and used by the `HashicorpVaultClient` to receive the `client_token` for the request authentication.
More on that in the sections [Hashicorp Vault Auth Implementation Registration](#Hashicorp-Vault-Auth-Registration) and [HashiCorp Vault Client](#HashiCorp-Vault-Client)

### Haschicorp Vault Auth Service Extension

For every authentication method, an implementation of the [Hashicorp Vault Auth interface](#hashicorp-vault-auth-interface) is needed.
Each `HashicorpVaultAuth` implementation represents a different authentication method and is packaged inside its own service extension.
In this way, it can easily be added/removed from the runtime and maintained in one place.
Due to the possible differences in the needed configuration of different authentication methods, each Service Extension will need its own configuration values specific to the authentication method.

### Simple Token Auth

To keep the HashiCorp Vault Extension functional on its own for demo and testing purposes, `SimpleTokenAuth` is implemented.
`SimpleTokenAuth` is a very rudimentary implementation of the [Hashicorp Vault Auth interface](#hashicorp-vault-auth-interface), that only stores a token and does not offer any refresh functionality.
It is always added to the registry and will be used when `fallbackToken` is selected as authentication method, more on that in the [Configuration](#configuration) section.

```java
public class SimpleTokenAuth implements HashicorpVaultAuth {
    private String vaultToken;

    SimpleTokenAuth(String vaultToken) {
        this.vaultToken = vaultToken;
    }

    @Override
    public String vaultToken() {
        return vaultToken;
    }
}
```

### Hashicorp Vault Auth Registry

In line with the registry pattern, `HashicorpVaultAuthRegistry` and `HashicorpVaultAuthRegistryImpl` are created.
The `HashicorpVaultAuthRegistry` will be used to store one or more implementations of `HashicorpVaultAuth`, each representing a different authentication method.
More on the usage of the `HashicorpVaultAuthRegistry` for registration in the [HashiCorp Vault Auth Registration](#hashicorp-vault-auth-registration) section.
The `HashicorpVaultAuthRegistryImpl` is added to the constructor of `HashicorpVaultClient` and can then be used to retrieve the `vaultToken` for the Header creation, more in that in the [HashiCorp Vault Client](#hashicorp-vault-client) section.

```java
public interface HashicorpVaultAuthRegistry {
    
    void register(String method, HashicorpVaultAuth authImplementation);
    
    @NotNull
    HashicorpVaultAuth resolve(String method);
    
    boolean hasMethod(String method);
}
```

```java
public class HashicorpVaultAuthRegistryImpl implements HashicorpVaultAuthRegistry {
    
    private final Map<String, HashicorpVaultAuth> services = new HashMap<>();

    public HashicorpVaultAuthRegistryImpl() {
    }

    @Override
    public void register(String method, HashicorpVaultAuth service) {
        services.put(method, service);
    }

    @Override
    public @NotNull HashicorpVaultAuth resolve(String method) {
        if (services.get(method) == null){
            throw new IllegalArgumentException(String.format("No authentication method registered under %s", method));
        }
        return services.get(method);
    }

    @Override
    public boolean hasMethod(String method) {
        return services.containsKey(method);
    }
}
```

### Hashicorp Vault Auth Registration

During the `initialize()` call, service extensions providing an auth method will register an instance of their `HashicorpVaultAuth` implementation in the `HashicorpVaultAuthRegistry`.
The `HashicorpVaultAuthRegistry` is provided to the service extension through use of the provider pattern by the `HashicorpVaultExtension`.

```java
@Provider
public HashicorpVaultAuthRegistry hashicorpVaultAuthRegistry() {
    return new HashicorpVaultAuthRegistryImpl();
}
```

Inside the service extension providing an `HashicorpVaultAuth` implementation, the `HashicorpVaultAuthRegistry` is injected.

```java
@Inject
private HashicorpVaultAuthRegistry hashicorpVaultAuthRegistry;
```

The injected `HashicorpVaultAuthRegistry` is used to register the `HashicorpVaultAuth` implementation.

```java
@Override
public void initialize(ServiceExtensionContext context) {
    var token = context.getSetting(VAULT_TOKEN, null);
    
    if (hashicorpVaultAuthRegistry.hasMethod("token-based")) {
        throw new EdcException("Authentication method token-based is already registered");
    }

    hashicorpVaultAuthRegistry.register("token-based", HashicorpVaultTokenAuth(token));
    
}
```

### Configuration

A new config value is introduced to the HashiCorp Vault Extension named `edc.vault.hashicorp.auth.method`.
`edc.vault.hashicorp.auth.method` governs which `HashicorpVaultAuth` implementation is used from `HashicorpVaultAuthRegistry` and is persisted in `HashicorpVaultSettings`.

For testing and demo purposes, another setting called `edc.vault.hashicorp.auth.token.fallback` is introduced.
In case, the HashiCorp Vault Extension is used on its own, this setting can be used to store a token for authentication without any refresh mechanism.

### HashiCorp Vault Client

Since the `HashicorpVaultClient` contains a lot of authentication logic, it will also go through a refactoring.
The goal of the refactoring, is the removal of the token authentication logic from `HashicorpVaultClient` and to make the authentication logic interchangeable.
`VaultAuthenticationRegistry` is passed to `HashicorpVaultClient` during creation by `HashicorpVaultServiceExtension`.
`HashicorpVaultClient` will use `VaultAuthenticationRegistry` based on `edc.vault.hashicorp.auth.method` setting to fetch the `client_token` that is provided by the chosen `HashicorpVaultAuth` implementation.
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
private Headers getHeaders() {
    var headersBuilder = new Headers.Builder().add(VAULT_REQUEST_HEADER, Boolean.toString(true));
    headersBuilder.add(VAULT_TOKEN_HEADER, vaultAuthenticationRegistry.resolve(settings.getAuthMethod()).vaultToken());
    return headersBuilder.build();
}
```

An important change that was made, is the point at which the headers are generated.
Previously, the headers were generated only once since the token was not liable to change.
With the addition of numerous potential authentication mechanisms, it can no longer be guaranteed, that the token never changes during refresh.
An example for this would be the kubernetes authentication method, where only short term tokens are produced each time.
As such, `headers` are no longer saved as a variable inside the `HashicorpVaultClient` and `getHeaders()` is called instead to always fetch the newest token.

### HashiCorp Vault Extension refactor

The token based authentication logic is refactored and moved into its own extension named `HashiCorp Vault Token Auth`.
This includes the token refresh functionality and will lead to a refactoring of the following classes:

`HashicorpVaultExtension.java`: `initialize()` no longer will start the token refresh and `stop()` will be removed since the extension does not govern token refresh anymore.

`HashicorpVaultClient.java`: `isTokenRenewable()` and `renewToken()` and their private methods will be moved to a new dedicated client in the Token Auth Extension.

`HashicorpVaultSettings.java`: some settings are moved and two new ones are added as described in the sections [HashiCorp Vault Token Auth Extension Settings](#hashicorp-vault-token-auth-extension-settings) and [Configuration](#configuration).

`HashicorpVaultTokenRenewTask.java`: is moved to the HashiCorp Vault Token Auth Extension in its entirety.

The respective tests covering the moved functionality are also moved accordingly.

### HashiCorp Vault Token Auth Extension

The HashiCorp Vault Token Auth Extension will contain the token storage functionality and the token refresh functionality that has been removed from the HashiCorp Vault Extension.

#### HashiCorp Vault Token Auth Extension Settings

The following settings will be moved from the HashiCorp Vault Extension to th HashiCorp Vault Token Auth Extension.

Duplicate setting from the HashiCorp Vault Extension, since the HashiCorp Vault Token Auth Extension also needs access to the vault.
`edc.vault.hashicorp.url`

Settings moved from the HashiCorp Vault Extension, that only concern the token storage and token refresh.
`edc.vault.hashicorp.token`
`edc.vault.hashicorp.token.scheduled-renew-enabled`
`edc.vault.hashicorp.token.ttl`
`edc.vault.hashicorp.token.renew-buffer`

### HashiCorp Vault Health Extension

The `get()` method of `HashicorpVaultHealthCheck` is currently using a merge of the response from the Hashicorp Vault Health API and the ability to renew the given token.
After the refactor, the token renewal is no longer a part of the base `HashicorpVaultExtension`.
As such the `get()` method is adjusted and will only return the response from the Hashicorp Vault Health API.
