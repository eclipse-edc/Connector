# Cloud testing

## Usage

### Overview

A cloud deployment is required to test the integration with resources that cannot be run in a local emulator, such as Azure Data Factory.

- A **GitHub environment** is used to encapsulate secrets.
- An **application** is created to represent the system when running test. In Azure Active Directory, a service principal for the application is configured in the cloud tenant, and configured to trust the GitHub environment using Federated Identity Credentials. For running tests locally, developers should be assigned equivalent or higher permissions.
- **Cloud resources** and **role assignments** are provisioned using Terraform. To simplify the configuration and reduce the risk of leakage of elevated credentials, Terraform is executed manually by privileged developers on their local machine (rather than in a GitHub workflow).
- **Configuration** for connecting to cloud resources is provided using `.properties` file, it is build upon Terraform outputs JSON. The file is uploaded to GitHub as an Environment secret. For running tests locally, a script is provided to download this file locally.

### Forks and pull requests

The cloud testing pipeline detects whether credentials for an Azure environment are configured. If not, the cloud testing pipeline is skipped.

When running pull requests across repositories (from a repository fork), the workflow doesn't have access to secrets for security reasons. Therefore, the cloud testing pipeline will only run after the PR is merged.

If the PR author has reason to expect that the PR may break cloud tests, they should configure credentials for an Azure environment on their fork and provision Azure resources. This will cause the cloud testing workflow to run on their fork (outside of PR checks). The author should attach evidence of the cloud testing workflow run to the PR.

Alternatively, the reviewer from the upstream repository may pull the PR into a temporary branch on the upstream repository in order to trigger the cloud testing workflow (outside of PR checks). This should only be done after careful inspection that the code is not leaking credentials.

## Deploying an Azure environment

### Planning your deployment

You will need:

- An Azure subscription
- At least one developer with the `Owner` role on the Azure subscription in order to deploy resources and assign roles
- A service principal (instructions below)
- For developers to be able to read configuration from Terraform outputs, grant them the `Storage Blob Data Reader` (or `Contributor`) role on the storage account holding the Terraform state file.
- For developers to be able to use their own identity to run tests, grant the following roles at the Subscription level, or on the Resource Group created to hold Terraform resources.
  - `Storage Account Contributor` (or `Contributor`)
  - `Data Factory Contributor` (or `Contributor`)
  - `Key Vault Secrets Officer`
- Developers will need the following utilities installed locally:
  - [Azure CLI](https://docs.microsoft.com/cli/azure/install-azure-cli)
  - [Terraform CLI](https://learn.hashicorp.com/tutorials/terraform/install-cli)
  - [GitHub CLI](https://cli.github.com)

### Log in to Azure & GitHub

- Both for running Terraform and running tests in a local development environment, you must be [signed in to the target Azure subscription with the Azure CLI](https://docs.microsoft.com/cli/azure/authenticate-azure-cli) and [have the target Azure subscription selected](https://docs.microsoft.com/cli/azure/manage-azure-subscriptions-azure-cli).

- You must be [signed in to GitHub with the GitHub CLI](https://cli.github.com/manual/gh_auth_login) and must have Contributor permissions on the repository.

### Create a service identity for the GitHub Environment

[Create and configure an application for your GitHub Environment](https://docs.microsoft.com/azure/active-directory/develop/workload-identity-federation-create-trust-github).

Follow the instructions to *Create an app registration*.

- In **Supported Account Types**, select **Accounts in this organizational directory only**.
- Don't enter anything for **Redirect URI (optional)**.

Take note of the Application (client) ID.

Create a client secret by following the section "Create a new application secret" in the page on [Creating an Azure AD application to access resources](https://docs.microsoft.com/en-us/azure/active-directory/develop/howto-create-service-principal-portal#option-2-create-a-new-application-secret). Take note of the client secret and keep it safe.

Follow the instructions to *Configure a federated identity credential*.

- For **Entity Type**, select **Environment**.
- For **Environment Name**, type `Azure-dev`. That is the environment name hard-coded in the workflow definition.
- For **Name**, type any name.

Note that the GitHub Environment itself is created automatically when used in a workflow, you don't need to take any action to create the Environment in your GitHub repository.

### Configure Terraform settings

The shell scripts that wrap Terraform commands take their configuration from a file named `.env` that should not be committed into the repository (though the file should be shared across developers in your fork). Copy and adapt the example settings file to your environment, following the comments in the file:

```bash
cp resources/azure/testing/.env.example resources/azure/testing/.env
```

At a minimum, you must modify the following values:

- `GITHUB_REPO` (to reflect your fork)
- `TERRAFORM_STATE_STORAGE_ACCOUNT` (must be globally unique)
- `TF_VAR_ci_client_id` (using the App registration created in the previous step)
- `TF_VAR_prefix` (must be globally unique)

### Deploying Terraform resources

The first time only, set up the state storage account by running this script:

```bash
resources/azure/testing/terraform-initialize.sh
```

After that, run this script to update cloud resources. Follow the prompt and enter `yes` if requested to apply any changes:

```bash
resources/azure/testing/terraform-apply.sh
```

The script also configures your repository's GitHub Environment so that workflows can consume the resources. The following secrets are provisioned in the Environment:

- `AZURE_TENANT_ID`, `AZURE_CLIENT_ID` , and `AZURE_SUBSCRIPTION_ID`, required to log in with the Federated Credential scenario.
- `RUNTIME_SETTINGS`, a multiline string in key/value configuration file format containing resource identifiers and other settings needed to connect tests to the resources deployed with Terraform.

Note that these values do not actually contain any sensitive information.

That is sufficient to have the cloud testing workflow run in your fork on every Git push.

### Consuming Terraform resources locally

For running cloud tests in local development, run this script to download a `runtime_settings.properties` file:

```bash
resources/azure/testing/terraform-fetch.sh
```

This downloads a `runtime_settings.properties` file, which is read by cloud integration tests. This file should not be committed to the repository.

### Evolving Terraform configuration

When doing changes to the Terraform configuration, keep in mind that the cloud resources, as well as the Terraform output values, are shared with other branches. You must ensure that any changes are backward compatible.

For example, if changing a test so that a resource is not needed anymore, do not remove the resource from the Terraform configuration in the same PR.

### Deleting Terraform resources

Resources such as pipeline definitions created in Data Factory prevent the use of Terraform to destroy the created resources. However, the deletion of the storage account for Terraform state and the resource group containing generated Azure resources can be scripted.
