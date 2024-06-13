# Token Based Authentication Service

The token based authentication service extension is used to secure connector APIs. These APIs are not protected by the `AuthenticationService` by default. To find out how a specific API is protected please consult its documentation.

APIs, protected by this extension, require a client to authenticate by adding a authentication key to the request header.

Authentication Header Example:
```
curl <url> --header "X-API-Key: <key>"
```

## Configuration

| Key                    | Description                                                  | Required |
|:-----------------------|:-------------------------------------------------------------|:---------|
| edc.api.auth.key       | API Key Header Value                                         | false    |
| edc.api.auth.key.alias | Secret name of the API Key Header Value, stored in the vault | false    |

- If the API key is stored in the Vault _and_ in the configuration, the extension will take the key from the vault.

- If no API key is defined, a random value is generated and printed out into the logs.