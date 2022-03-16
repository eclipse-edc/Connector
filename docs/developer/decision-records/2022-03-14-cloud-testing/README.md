# Cloud testing

## Decision

A cloud deployment is used to test the integration with resources that cannot be run in a local emulator, such as Azure Data Factory.

The cloud testing pipeline detects whether credentials for an Azure environment are configured. If not, the cloud testing pipeline is skipped. This allows forks to opt-in to the cloud testing feature, by deciding whether to set up credentials.

The necessary resources are provisioned using Terraform. To simplify the configuration and reduce the risk of leakage of elevated credentials, Terraform is executed manually by privileged developers on their local machine (rather than in a GitHub workflow).

When doing changes to the Terraform configuration, developers must keep in mind that the cloud resources, as well as the Terraform output values, are shared with other branches. Changes must be backward compatible.
