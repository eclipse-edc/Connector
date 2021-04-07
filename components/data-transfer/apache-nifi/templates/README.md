# Sample template for copying blobs between Azure storages

`TwoClouds.xml` is a sample template that can copy blobs between Azure Storage and Adls Gen2.

The template requires credentials to the certificate (apiStorePasswd, apiKeyPasswd) see [NiFi Setup](../../NiFiSetup.md) and credentials (apiUsername, apiPassword) for triggering the flow using Http post request.
`InsertCredentials.xslt` can be used to insert the credentials into the template with any xslt tool, e.g. `xsltproc`.

``` bash
apiUsername=<Api user name>
apiPassword=<Api password>

xsltproc --stringparam apiUsername $apiUsername --stringparam apiPassword $apiPassword -o bld/TwoClouds.xml InsertCredentials.xslt TwoClouds.xml
```

`bld/TwoClouds.xml` will have resulting template with the credentials ready to be deployed to NiFi.

## Deploying a template to NiFi

- Open NiFi web app.
- In the Operate block click upload template icon.
- Select a template to upload (e.g. `TwoClouds.xml`).
- Click Upload.
- Drag and drop Template icon from the tool bar onto empty canvas.
- Chose the template from the list.

When a template is instantiated controller services must be enabled and the flow started.

- Doubleclick `HandleHttpRequest` processor and click an arrow next to StandardHttpContextMap. It will open a new window with the services.
- Enable the service, there must be exactly one.
- Close the window.
- Make sure that no processors are selected and click triangle start button. It should start all processors.

## Invoking the flow

You can test the flow invoking the following:

``` bash
azStorageSas="<source storage account SAS>"
adlsSas="<destination storage account SAS>"

jq '.source += {"sas":"'$adlsSas'"}' AdlsToAzS.json | jq '.destination += {"sas":"'$azStorageSas'"}' -c > bld/AdlsToAzS.json
jq '.source += {"sas":"'$azStorageSas'"}' AzSToAdls.json | jq '.destination += {"sas":"'$adlsSas'"}' -c > bld/AzSToAdls.json

curl -u ${apiUsername}:${apiPassword} -H "Content-Type: application/json" -d @bld/AdlsToAzS.json -X POST <NiFi Url>/contentListener -w "\n%{http_code}\n"
```
