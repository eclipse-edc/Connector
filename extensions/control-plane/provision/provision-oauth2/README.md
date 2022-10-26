# OAuth2 Provision

Permits to attach an OAuth2 token to the transfer, needed when the source or the sink require it.

## How it works
![](docs/diagram.png)

The token will be requested in the provision phase and stored in the vault, then it will be used by the `data-plane-http`
extension and put in the request.

## How to use it

The extension works all the `HttpData` addresses that contain the properties (defined in 
[Oauth2DataAddressSchema](src/main/java/org/eclipse/dataspaceconnector/provision/oauth2/Oauth2DataAddressSchema.java)):
- `oauth2:clientId`: the client id 
- `oauth2:clientSecret`: the client secret
- `oauth2:tokenUrl`: the url where the token will be requested

