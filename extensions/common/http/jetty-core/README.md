# Jetty

This extension provides a `JettyService`, a Servlet Context Container that can expose REST API on a Jersey based
WebServer.

## Terminate SSL in the connector runtime

While it is generally advised to handle HTTPS/SSL/TLS termination through deployment mechanisms such as
Kubernetes, or an API gateway, there are situations where SSL termination directly in the connector is
needed.

In order to do that, users must set up a Java keystore (
see [Jetty documentation](https://www.eclipse.org/jetty/documentation/jetty-10/operations-guide/index.html#og-keystore))
that contains the certificate. Both official and self-signed certificates work.

The following Java properties can be used to configure Jetty to actually use the keystore:

| Config Key                        | Description                                                   | Required | Default    |
|-----------------------------------|---------------------------------------------------------------|----------|------------|
| `edc.web.https.keystore.password`   | Password for the keystore                                     | yes      | "password" |
| `edc.web.https.keymanager.password` | Password for the private key                                  | yes      | "password" |
| `edc.web.https.keystore.path`       | Absolute path on the filesystem where the keystore is located | yes      | null       |
| `edc.web.https.keystore.type`       | Type of keystore, could be "JKS" or "PKCS12"                  | no       | "PKCS12"     |

## Expose different web contexts

By default we have the `web.http.port`
and `web.http.path` config entry with the following default values:

```properties
web.http.port=8181
web.http.path=/api
```

That will expose any resource under `http://<host>:8181/api/*`.

In some situations it is required to expose a part of the public API under another port or path, which can be achieved
by using port mappings. First, a "named"
property group needs to be created using the following configuration. The example will create a port mapping with the
name/alias `"health"`:

```properties
web.http.health.port=9191
web.http.health.path=/api/v1/health
```

Second, the resource must be registered using that context alias (`"health"`):

```java
public class YourExtension {

    @Inject
    private WebService webService;

    void initialize(ServiceExtensionContext context) {
        //...
        webService.registerResource("health", new HealthController());
    }
}
```

which will expose any resources that the `HealthController` provides under `http://<host>:9191/api/v1/health/*`

## Best practice

In situations where an API is made up of multiple controllers, it is best to expose the APIs base path using this
mechanism, register the controllers with the
port mapping's context alias and have the controllers themselves handle any sub-paths.

## Limitations

- Attempting to register two port mappings with the same port will raise an exception. This also takes into account the
  implicit default values
- Attempting to register two port mappings with the same path will raise an exception, even if different ports are used.
  This has to do with the way how servlet
  contexts are mapped internally and may change in future releases.
