# Project

> The Data Appliance GX project is intended as a proving ground for GAIA-X and data transfer techologies.

# Getting Started

The project requires JDK 11+. To get started:

``` git clone https://github.com/microsoft/Data-Appliance-GX ```

``` cd Data-Appliance-GX ```

```./gradlew clean shadowJar```

To launch the runtime and client from the root build directory, respectively:

```java -jar runtime/build/libs/dagx-runtime.jar```

```java -jar client/build/libs/dagx-client.jar```

# Build Profiles

The runtime can be configured with custom modules be enabling various build profiles.

By default, no vault is configured. To build with the file system vault, enable the security profile:

```./gradlew -Dsecurity.type=fs clean shadowJar ```

The runtime can then be started from the root clone directory using:

``` java -Ddagx.vault=secrets/dagx-vault.properties -Ddagx.keystore=secrets/dagx-test-keystore.jks -Ddagx.keystore.password=test123 -jar runtime/build/libs/dagx-runtime.jar ```

Note the secrets directory referenced above is configured to be ignored. A test key store and vault must be added (or
the launch command modified to point to different locations). Also, set the keystore password accordingly.

# A word on distributions

The code base is organized in many different modules, some of which are grouped together using so-called "feature
bundles". For example, accessing IDS requires a total of 4 modules, which are grouped together in the `ids` feature
bundle. So developers wanting to use that feature only need to reference `ids` instead all of the 4 modules
individually. This allows for a flexible and easy composition of the runtime. We'll call those compositions "
distributions".

A distribution basically is a Gradle module that - in its simplest form - consists only of a `build.gradle.kts` file
which declares its dependencies and how the distribution is assembled, e.g. in a `*.jar` file, as a native binary, as
docker image etc. It may also contain further assets like configuration files. An example of this is shown in
the `distributions/demo` folder.

# Building and running with Docker

We suggest that all docker interaction be done with a Gradle plugin, because it makes it very easy to encapsulate
complex docker commands. An example of its usage can be seen in `distributions/demo/build.gradle.kts`

The docker image is built with

```shell
./gradlew clean buildDemo
```

which will assemble a JAR file that contains all required modules for the "Demo" configuration (i.e. file-based config
and vaults). It will also generate a `Dockerfile` in `build/docker` and build an image based upon it.

The container can then be built and started with

```shell
./gradlew startDemo
```

which will launch a docker container based on the previously built image.

# Setup Azure resources

A working connector instance will use several resources on Azure, all of which can be easily deployed using a so-called
"genesis script" located at `./scripts/genesis.sh`. Most Azure resources are grouped together in so-called "resource
groups" which makes management quite easy. The Genesis Script will take care of provisioning the most essential
resources and put them in a resource group:

- KeyVault
- Blob Store Account
- an AKS cluster
- two app registrations / service principals

App registrations are used to authenticate apps against Azure AD and are secured with certificates, so before
provisioning any resources, the Genesis Script will generate a certificate (interactively) and upload it to the app
registrations.

The script requires Azure CLI (`az`) and `jq` (a JSON processor) and you need to be logged in to Azure CLI. Once that's
done, simply `cd scripts` and invoke

```bash
./genesis.sh <optional-prefix>
```

the `<optional-prefix>` is not necessary, but if specified it should be a string without special characters, as it is
used as resource suffix in the Azure resources. If omitted, the current Posix timestamp is used.

After having completed its work, which could take >10mins, the scripts can automatically cleanup resources, please
observe the cli output.

_Note that the Genesis Script does not deploy any applications such as Nifi, this is handled in a second stage!_

## Contributing

This project welcomes contributions and suggestions. Most contributions require you to agree to a Contributor License
Agreement (CLA) declaring that you have the right to, and actually do, grant us the rights to use your contribution. For
details, visit https://cla.opensource.microsoft.com.

When you submit a pull request, a CLA bot will automatically determine whether you need to provide a CLA and decorate
the PR appropriately (e.g., status check, comment). Simply follow the instructions provided by the bot. You will only
need to do this once across all repos using our CLA.

This project has adopted the [Microsoft Open Source Code of Conduct](https://opensource.microsoft.com/codeofconduct/).
For more information see the [Code of Conduct FAQ](https://opensource.microsoft.com/codeofconduct/faq/) or
contact [opencode@microsoft.com](mailto:opencode@microsoft.com) with any additional questions or comments.

## Trademarks

This project may contain trademarks or logos for projects, products, or services. Authorized use of Microsoft trademarks
or logos is subject to and must follow
[Microsoft's Trademark & Brand Guidelines](https://www.microsoft.com/en-us/legal/intellectualproperty/trademarks/usage/general)
. Use of Microsoft trademarks or logos in modified versions of this project must not cause confusion or imply Microsoft
sponsorship. Any use of third-party trademarks or logos are subject to those third-party's policies.
