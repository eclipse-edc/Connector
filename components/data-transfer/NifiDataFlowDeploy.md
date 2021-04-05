# How to deploy a sample data flow to nifi

In order to deploy an existing data flow template into a Nifi cluster, we are using the Nifi Rest api. In this directory we have created a `.http` file that contains the required calls to deploy and initiate a template. You could use VSCode's rest client to trigger all the calls and to inspect the responses.

## .http & .env Setup

You would need to copy/create a .env file to pass in the required variable to the .http file. A sample file is created in the same directory which could be copied and renamed. When the Nifi cluster is deployed you can obtain the cluster url and set the `nifi_FQDN` variable in the .env file.

For the simplicity we have also placed the sample data flow xml file in the same directory.

Here is the link to the [Nifi Rest Api.](https://nifi.apache.org/docs/nifi-docs/rest-api/index.html)
