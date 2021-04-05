# How to setup and configure NiFi in AKS

If you are creating AKS from Azure Portal there is a step where you can enable HTTP routing. Check the box enabling it. If the cluster already exists, check "Enable HTTP application routing" checkbox on the Networking tab.
When AKS cluster is deployed it creates a new resource group with AKS specific resources. The name of the resource group starts with `MC_` and contains the AKS name as a part. One of the resources is a public IP having a name that starts with `kubernetes-`. Open that resource in Azure Portal, go to Configuration tab, `DNS name label` parameter and enter a prefix for the NiFi url. Take a note of the `NiFi FQDN`, e.g. `MyNiFi.westus.cloudapp.azure.com`.

Make sure that you have Azure CLI installed.

Run:

``` bash
az aks get-credentials --name <AKS name> --resource-group <resource group>
```

it will enable access to the AKS cluster.

## Setup NiFi

Git clone [https://github.com/sushilkm/nifi-chart](https://github.com/sushilkm/nifi-chart)

### Low memory settings

For a small cluster adjust NiFi memory requirements. Edit `nifi/values.yaml` Set:

``` yaml
nifi.bootstrapConf.jvmMinMemory: 3g
nifi.bootstrapConf.jvmMaxMemory: 3g
resources.requests.memory: "1Gi"
```

or use some other small enough values.

### Configure ingress

In the same `nifi/values.yaml` file set:

``` yaml
ingress.enabled: true
nifi.properties.webProxyHost: <NiFi FQDN>
```

Edit `nifi/templates/ingress.yaml` Add:

``` yaml
metadata.annotations.kubernetes.io/ingress.class: addon-http-application-routing
```

### Configure admin users

Edit `nifi/values.yaml` Set:

``` yaml
initUsers.enabled: true
admins:
    - <your AAD login e-mail>
uiUsers:
    - <your AAD login e-mail>
```

## Configure NiFi SSL access to flows

To expose a port where NiFi flow can accept http requests edit `nifi/templates/service.yaml` and add the following to the 2nd template (the service with the name: `name: {{ template "nifi.fullname" . }}`) `spec.ports` section.
Make sure you add it after the `{{- end }}` statement related to the SSL if, `{{- if .Values.nifi.properties.secured }}`, so that the port is always exposed:

``` yaml
- port: 8888
    name: httplisten
    targetPort: 8888
```

Edit `nifi/templates/ingress.yaml` and add the following to the `spec.http.paths` section:

``` yaml
- backend:
    serviceName: {{ template "nifi.fullname" . }}
    servicePort: 8888
  path: /contentListener
```

Replace all three 8888 entries with a port number that must be exposed.

## Configure NiFi SSL Context Service

SSL Context Service enables https access to NiFi processors such as ListenHttp. SSL Context Service requires a certificate and
by default NiFi has an SSL certificate in the keystores that can be used but on every kubernetes node it is encrypted with a different password.
One solution is creating a copy of the certificate on each node encrypted with the same password.

`nifi/templates/statefulset.yaml` defines a container named `server` and the `command` section creates key stores for different scenarios. The passwords
are stored on the node as plain text and are available to the script. Find `# Update nifi.properties for security properties` line and after the code block that
follows add one more:

``` yaml
# create copy for api webserver
keyStore=${NIFI_HOME}/config-data/certs/keystore.jks
apiKeyStore=${NIFI_HOME}/config-data/certs/apikeystore.jks
cp $keyStore $apiKeyStore
keystorePasswd=$(jq -r .keyStorePassword ${NIFI_HOME}/config-data/certs/config.json)
keyPasswd=$(jq -r .keyPassword ${NIFI_HOME}/config-data/certs/config.json)

apiStorePasswd="<some password>"
apiKeyPasswd="<some password>"

keytool -storepasswd -storepass $keystorePasswd -new $apiStorePasswd -keystore $apiKeyStore -storetype JKS
keytool -keypasswd -alias "nifi-key" -storepass $apiStorePasswd -keypass $keyPasswd -new "$apiKeyPasswd" -keystore $apiKeyStore -storetype JKS

```

Replace `<some password>` with a certificate password. The password will be required to setup http processors in NiFi. For dev environment use a hardcoded value

### Configure Azure AD authentication

Follow "Deploying NiFi with authentication using OpenID Connect and Azure AD" section in the `/README.md` for updating `nifi/openid-values.yaml`

Now you can run

``` bash
make deploy-secured-nifi-with-openid-authentication-with-toolkit
```

Complete the steps in the "Deploying NiFi with authentication using OpenID Connect and Azure AD" section.
When `make` finishes the output will say to update `hosts` file but it is not necessary because Public Azure IP is associated with the proper DNS name.
The output will also show a command for getting the IP address of the NiFi UI, similar to:

``` bash
kubectl get ingress <ingress name> -n <namespace> -o jsonpath='{.status.loadBalancer.ingress[*].ip}' | xargs echo
```

Run it and verify that the IP matches public IP for the AKS that you configured in the beginning.

NiFi web ui should be available at `https://<NiFi FQDN>/nifi/` Because by default NiFi uses a self signed cert the URL is not secure and certificate errors
have to be ignored explicitly.

## Create test NiFi flow

- Open `https://<NiFi FQDN>/nifi/` in a browser ignoring certificate warnings and login.
- It should open a Microsoft AAD login window. Login with `your AAD login e-mail` configured above.
- By default you will not have the permissions. Click the `key` icon in the `operate` box and add your AAD
  login to all policies, creating policies as needed. Click the sandwich menu icon in the top right corner,
  chose policies and add yourself there as well. For a dev environment add yourself to all policies that are available.
- Create ListenHttp processor.
- Configure the processor matching the values such as port and path with the values specified in `Configure NiFi SSL access to flows`
(port: 8888, path: `contentListener` without slashes, health check port can be set to any other value).
- Configure SSL Context Service for the processor using StandardRestrictedSSLContextService. You will need the keystore path (`/opt/nifi/nifi-current/config-data/certs/apikeystore.jks`) and the certificate password that you set in the section above.
  - Set the `Keystore Filename` to the keystore path
  - Set the `Key Password` and `Keystore Password` to the values set in the section above
  - Set the `Keystore Type` to `JKS`
  - Enable the Controller Service by pressing the thunderbolt icon on the `Controller Service` page (After you close the `Controller Service Details` dialog)
- Start the processor.

In a terminal run:

``` bash
curl -k -X POST https://<NiFi FQDN>/contentListener -w "%{http_code}\n"
```

The command should return 200.
