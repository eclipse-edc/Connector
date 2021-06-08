# Deploy an example configuration with Terraform

It is assumed that the reader has a basic understanding of the following topics:
- Azure
- Kubernetes + Helm Charts
- Hashicorp Terraform

## Create a certificate for the main security principal
The main security principal is the security context that is used to identify the connector agains Azure AD and uses OAuth2 Client Credentials flow.
In order to make this as secure as possible the connector authenticates using a certificate. Thus, a `.pem` certificate is required.

For development purposes a self-signed certificate can be created by executing the following commands on the command line:
```bash
openssl req -newkey rsa:4096 -new -nodes -x509 -days 3650 -keyout key.pem -out cert.pem
openssl pkcs12 -inkey key.pem -in cert.pem -export -out cert.pfx
```
This generates a certificate (`cert.pem`), a private key (`key.pem`) and it also converts the `*.pem` certificate to a "pixie" (=`*.pfx`) certificate, because the Azure
libs require that.

**For now it is required that the certificate is named `"cert.pem"` and is located at the root directory `terraform/`.**

## Login to the Azure CLI
Install Azure CLI and execute `az login` on a shell.

## Initialize Terraform
Terraform must be installed. Then download the required providers by executing `terraform init`

## Deploy the cluster and associated resources
Users can run `terraform plan` to create a "dry-run", which lists all resources that will be created in Azure. This is not required, but gives a good
overview of what is going to happen.

The actual deployment is triggered by running 
```bash
terraform apply
```
which will prompt the user to enter a value for `resourcesuffix`. It is best to enter a short identifier without special characters, e.g. `test123`. 
 
The terraform project will then deploy three resource groups in Azure:
-  `dagx-<suffix>-resources`: This is where the key vault and the blobstore will be
- `dagx-<suffix>-cluster`: will contain the AKS cluster
- `MC-dagx-<suffix>-cluster_dagx-<suffix>-cluster_<region>`: will contain networking resources, virtual disks, scale sets, etc.

`<suffix>` refers to a parameter that can be specified when running `terraform apply`. It simply is a name that is used to identify resources.
`<region>` is the geographical region of the cluster and associated resources. Can be specified by running `terraform apply -var 'region=eastus'`.

**It takes quite a long time to deploy all resources, 5-10 minutes at least!**

## Configure a DNS name (manually)
At this point it is required that the DNS name for the cluster load balancer's ingress route (IP) is configured manually. 
In the resource group whos name begins with `MC-dagx-<suffix>...` there should be a public ip address, whos name starts with `kubernetes_...`.

Open that, open its Configuration and in the `DNS name label` field enter `dagx-<suffix>`, so for example `dagx-test123` so the resulting DNS name (=FQDN)
should be `dagx-test123.<region>.cloudapp.azure.com`.

## Re-using AKS credentials in kubernetes and helm
After the AKS is deployed, we must obtain its credentials before we can deploy any kubernetes workloads. Normally we would do that by running 
`az aks get-credentials -n <cluster-name> -g <resourcegroup>`.

However, since both the AKS and Nifi get deployed in one command (i.e. `terraform apply`), there is no chance to obtain credentials manually. According to
[this example from Hashicorp](https://github.com/hashicorp/terraform-provider-kubernetes/blob/main/_examples/aks/main.tf) it is good practice to deploy the AKS
and the workload in two different Terraform _contexts_ (=modules), which in our case are named `aks-cluster` and `nifi-config`. Basically this deploys the AKS, stores the credentials in a 
local file `kubeconfig` and the deploys Nifi re-using that config. 

