# Promoting an artifact in Azure Devops

After the artifacts are published, they must be [promoted](https://learn.microsoft.com/en-us/azure/devops/artifacts/feeds/views?view=azure-devops&tabs=nuget) to indicate that they are ready to use.

This can either be done manually or via the web API.

The [promote.sh](promote.sh) does this.

You will need to specify an Azure Personal Access Token with following permissions:
- Packaging
  - Read
  - Write
  - Manage

And use the script like this:

```sh
AZURE_PAT="<some pat>" VERSION="<version to promote>" ./promote.sh
```
