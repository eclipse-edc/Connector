#!/bin/bash
./gradlew clean

function move() {
  local SOURCE="${1}"
  local TARGET="${2}"

  mkdir -p "${TARGET}"
  rmdir "${TARGET}"

  echo "move ${SOURCE}"
  git mv "${SOURCE}" "${TARGET}"
}

function delete() {
  local DIR="${1}"

  echo "delete directory ${DIR}"
  rm -rf "${DIR}"
}

move "core/bootstrap" "common/core/bootstrap"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":core:bootstrap"/":common:core:bootstrap"/g' {} \;

move "core/protocol/web" "common/core/protocol/web"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":core:protocol:web"/":common:core:protocol:web"/g' {} \;

move "core/policy/policy-engine" "common/core/policy/policy-engine"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":core:policy:policy-engine"/":common:core:policy:policy-engine"/g' {} \;

move "core/policy/policy-model" "common/core/policy/policy-model"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":core:policy:policy-model"/":common:core:policy:policy-model"/g' {} \;

move "core/contract" "services/connector/core/contract"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":core:contract"/":services:connector:core:contract"/g' {} \;

move "core/schema" "services/connector/core/schema"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":core:schema"/":services:connector:core:schema"/g' {} \;

move "core/transfer" "services/connector/core/transfer"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":core:transfer"/":services:connector:core:transfer"/g' {} \;

move "core/build.gradle.kts" "services/connector/core/build.gradle.kts"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":core"/":services:connector:core"/g' {} \;

delete "core"

move "data-protocols/ids/ids-spi" "services/connector/extensions/ids/ids-spi"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":data-protocols:ids:ids-spi"/":services:connector:extensions:ids:ids-spi"/g' {} \;

move "data-protocols/ids/ids-core" "services/connector/extensions/ids/ids-core"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":data-protocols:ids:ids-core"/":services:connector:extensions:ids:ids-core"/g' {} \;

move "data-protocols/ids/ids-api-catalog" "services/connector/extensions/ids/ids-api-catalog"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":data-protocols:ids:ids-api-catalog"/":services:connector:extensions:ids:ids-api-catalog"/g' {} \;

move "data-protocols/ids/ids-api-multipart-dispatcher-v1" "services/connector/extensions/ids/ids-api-multipart-dispatcher-v1"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":data-protocols:ids:ids-api-multipart-dispatcher-v1"/":services:connector:extensions:ids:ids-api-multipart-dispatcher-v1"/g' {} \;

move "data-protocols/ids/ids-api-multipart-endpoint-v1" "services/connector/extensions/ids/ids-api-multipart-endpoint-v1"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":data-protocols:ids:ids-api-multipart-endpoint-v1"/":services:connector:extensions:ids:ids-api-multipart-endpoint-v1"/g' {} \;

move "data-protocols/ids/ids-api-rest-dispatcher-v1" "services/connector/extensions/ids/ids-api-rest-dispatcher-v1"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":data-protocols:ids:ids-api-rest-dispatcher-v1"/":services:connector:extensions:ids:ids-api-rest-dispatcher-v1"/g' {} \;

move "data-protocols/ids/ids-api-rest-endpoint-v1" "services/connector/extensions/ids/ids-api-rest-endpoint-v1"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":data-protocols:ids:ids-api-rest-endpoint-v1"/":services:connector:extensions:ids:ids-api-rest-endpoint-v1"/g' {} \;

move "data-protocols/ids/ids-api-transfer" "services/connector/extensions/ids/ids-api-transfer"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":data-protocols:ids:ids-api-transfer"/":services:connector:extensions:ids:ids-api-transfer"/g' {} \;

move "data-protocols/ids/ids-policy-mock" "services/connector/extensions/ids-policy-mock"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":data-protocols:ids:ids-policy-mock"/":services:connector:extensions:ids:ids-policy-mock"/g' {} \;

move "data-protocols/ids/ids-transform-v1" "services/connector/extensions/ids/ids-transform-v1"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":data-protocols:ids:ids-transform-v1"/":services:connector:extensions:ids:ids-transform-v1"/g' {} \;

move "data-protocols/ids/README.md" "services/connector/extensions/ids/README.md"
move "data-protocols/ids/build.gradle.kts" "services/connector/extensions/ids/build.gradle.kts"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":data-protocols:ids"/":services:connector:extensions:ids"/g' {} \;

delete "data-protocols"

move "extensions/api/control" "services/connector/extensions/api/control"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":extensions:api:control"/":services:connector:extensions:api:control"/g' {} \;

move "extensions/aws/s3/provision" "services/connector/extensions/transfer/provision/aws/s3"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/:extensions:aws:s3:provision/:services:connector:extensions:transfer:provision:aws:s3/g' {} \;

move "extensions/aws/s3/s3-schema" "services/connector/extensions/schemas/aws/s3"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/:extensions:aws:s3:s3-schema/:services:connector:extensions:schemas:aws:s3/g' {} \;

move "extensions/azure/assetindex-cosmos" "services/connector/extensions/asset-index/azure/cosmosdb"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":extensions:azure:assetindex-cosmos"/":services:connector:extensions:asset-index:azure:cosmosdb"/g' {} \;

move "extensions/azure/blob/api" "common/libraries/azure/blob-storage-api-lib"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":extensions:azure:blob:api"/":common:libraries:azure:blob-storage-api-lib"/g' {} \;

move "extensions/azure/blob/provision" "services/connector/extensions/transfer/provision/azure/blob-storage/provision"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":extensions:azure:blob:provision"/":services:connector:extensions:transfer:provision:blob-storage:provision"/g' {} \;

move "extensions/azure/blob/blob-schema" "services/connector/extensions/schemas/azure/blob-storage"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":extensions:azure:blob:blob-schema"/":services:connector:extensions:schemas:azure:blob-storage"/g' {} \;

move "extensions/azure/contract-definition-store-cosmos" "services/connector/extensions/contract/definition-store/azure/cosmosdb"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":extensions:azure:contract-definition-store-cosmos"/":services:connector:extensions:contract:definition-store:azure:cosmosdb"/g' {} \;

move "extensions/azure/contract-negotiation-store-cosmos" "services/connector/extensions/contract/negotiation-store/azure/cosmosdb"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":extensions:azure:contract-negotiation-store-cosmos"/":services:connector:extensions:contract:negotiation-store:azure:cosmosdb"/g' {} \;

move "extensions/azure/cosmos-common" "common/libraries/azure/cosmosdb-api-lib"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":extensions:azure:cosmos-common"/":common:libraries:azure:cosmosdb-api-lib"/g' {} \;

move "extensions/azure/events" "services/connector/extensions/transfer/listener/azure/event-grid-publisher"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":extensions:azure:events"/":services:connector:extensions:transfer:listener:azure:event-grid-publisher"/g' {} \;

move "extensions/azure/events-config" "services/connector/extensions/transfer/listener/azure/event-grid-publisher-config-lib"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":extensions:azure:events-config"/":services:connector:extensions:transfer:listener:azure:event-grid-publisher-config-lib"/g' {} \;

move "extensions/azure/fcc-node-directory-cosmos" "services/catalog/extensions/node-directory/azure/cosmosdb"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":extensions:azure:fcc-node-directory-cosmos"/":services:catalog:extensions:node-directory:azure:cosmosdb"/g' {} \;

move "extensions/azure/transfer-process-store-cosmos" "services/connector/extensions/transfer/store/azure/cosmosdb"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":extensions:azure:transfer-process-store-cosmos"/":services:connector:extensions:transfer:store:azure:cosmosdb"/g' {} \;

move "extensions/azure/vault" "services/connector/extensions/vault/azure/key-vault"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":extensions:azure:vault"/":services:connector:extensions:vault:azure:key-vault"/g' {} \;

move "extensions/catalog/catalog-service" "services/catalog/extensions/catalog-service"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":extensions:catalog:catalog-service"/":services:catalog:extensions:catalog-service"/g' {} \;

move "extensions/catalog/federated-catalog-cache" "services/catalog/extensions/catalog/federated-catalog-cache"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":extensions:catalog:federated-catalog-cache"/":services:catalog:extensions:federated-catalog-cache"/g' {} \;

move "extensions/catalog/federated-catalog-node" "services/catalog/extensions/catalog/federated-catalog-node"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":extensions:catalog:federated-catalog-node"/":services:catalog:extensions:federated-catalog-node"/g' {} \;

move "extensions/catalog/federated-catalog-spi" "services/catalog/extensions/catalog/federated-catalog-spi"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":extensions:catalog:federated-catalog-spi"/":services:catalog:extensions:federated-catalog-spi"/g' {} \;

move "extensions/dataloading/dataloading-asset" "services/connector/extensions/data-loading/assets"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":extensions:dataloading:dataloading-asset"/":services:connector:extensions:data-loading:assets"/g' {} \;

move "extensions/dataloading/dataloading-contractdef" "services/connector/extensions/data-loading/contract-definitions"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":extensions:dataloading:dataloading-contractdef"/":services:connector:extensions:data-loading:contract-definitions"/g' {} \;

move "extensions/dataloading/dataloading-spi" "services/connector/extensions/data-loading/spi"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":extensions:dataloading:dataloading-spi"/":services:connector:extensions:data-loading:spi"/g' {} \;

move "extensions/filesystem/configuration-fs" "common/extensions/configuration/file-system"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":extensions:filesystem:configuration-fs"/":common:extensions:configuration:file-system"/g' {} \;

move "extensions/filesystem/vault-fs" "common/extensions/vault/file-system"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":extensions:filesystem:vault-fs"/":common:extensions:vault:file-system"/g' {} \;

move "extensions/iam/daps/" "services/connector/extensions/iam/daps"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":extensions:iam:daps"/":services:connector:extensions:iam:daps"/g' {} \;

move "extensions/iam/distributed-identity/dummy-credentials-verifier" "services/id-registration/extensions/iam/distributed-identity/dummy-credentials-verifier"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":extensions:iam:distributed-identity:dummy-credentials-verifier"/":services:did-registration:extensions:iam:distributed-identity:dummy-credentials-verifier"/g' {} \;

move "extensions/iam/distributed-identity/identity-common-test" "services/id-registration/extensions/iam/distributed-identity/identity-common-test"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":extensions:iam:distributed-identity:identity-common-test"/":services:did-registration:extensions:iam:distributed-identity:identity-common-test"/g' {} \;

move "extensions/iam/distributed-identity/identity-did-core" "services/did-registration/extensions/iam/distributed-identity/identity-did-core"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":extensions:iam:distributed-identity:identity-did-core"/":services:did-registration:extensions:iam:distributed-identity:identity-did-core"/g' {} \;

move "extensions/iam/distributed-identity/identity-did-crypto" "services/did-registration/extensions/iam/distributed-identity/identity-did-crypto"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":extensions:iam:distributed-identity:identity-did-crypto"/":services:did-registration:extensions:iam:distributed-identity:identity-did-crypto"/g' {} \;

move "extensions/iam/distributed-identity/identity-did-service" "services/did-registration/extensions/iam/distributed-identity/identity-did-service"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":extensions:iam:distributed-identity:identity-did-service"/":services:did-registration:extensions:iam:distributed-identity:identity-did-service"/g' {} \;

move "extensions/iam/distributed-identity/identity-did-spi" "services/did-registration/extensions/iam/distributed-identity/identity-did-spi"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":extensions:iam:distributed-identity:identity-did-spi"/":services:did-registration:extensions:iam:distributed-identity:identity-did-spi"/g' {} \;

move "extensions/iam/distributed-identity/identity-did-web" "services/did-registration/extensions/iam/distributed-identity/identity-did-web"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":extensions:iam:distributed-identity:identity-did-web"/":services:did-registration:extensions:iam:distributed-identity:identity-did-web"/g' {} \;

move "extensions/iam/distributed-identity/registration-service" "services/id-registration/extensions/iam/distributed-identity/registration-service"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":extensions:iam:distributed-identity:registration-service"/":services:did-registration:extensions:iam:distributed-identity:registration-service"/g' {} \;

move "extensions/iam/distributed-identity/registration-service-api" "services/id-registration/extensions/iam/distributed-identity/registration-service-api"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":extensions:iam:distributed-identity:registration-service-api"/":services:did-registration:extensions:iam:distributed-identity:registration-service-api"/g' {} \;

move "extensions/iam/distributed-identity/build.gradle.kts" "services/id-registration/extensions/iam/distributed-identity/build.gradle.kts"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":extensions:iam:distributed-identity"/":services:did-registration:extensions:iam:distributed-identity"/g' {} \;

move "extensions/iam/distributed-identity/README.md" "services/did-registration/extensions/iam/distributed-identity/README.md"

move "extensions/iam/iam-mock" "common/extensions/iam/iam-mock"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":extensions:iam:iam-mock"/":common:extensions:iam:iam-mock"/g' {} \;

move "extensions/iam/oauth2/oauth2-core" "services/connector/extensions/iam/oauth2-core"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":extensions:iam:oauth2:oauth2-core"/":services:connector:extensions:iam:oauth2-core"/g' {} \;

move "extensions/iam/oauth2/oauth2-spi" "services/connector/extensions/iam/oauth2-spi"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":extensions:iam:oauth2:oauth2-spi"/":services:connector:extensions:iam:oauth2-spi"/g' {} \;

move "extensions/in-memory/assetindex-memory" "services/connector/extensions/asset-index/memory"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":extensions:in-memory:assetindex-memory"/":services:connector:extensions:asset-index:memory"/g' {} \;

move "extensions/in-memory/contractdefinition-store-memory" "services/connector/extensions/contract/definition-store/memory"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":extensions:in-memory:contractdefinition-store-memory"/":services:connector:extensions:contract:definitions-store:memory"/g' {} \;

move "extensions/in-memory/did-document-store-inmem" "services/did-registration/extensions/did-document-store/memory"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":extensions:in-memory:did-document-store-inmem"/":services:did-registration:extensions:did-document-store:memory"/g' {} \;

move "extensions/in-memory/fcc-node-directory-memory" "services/catalog/extensions/node-directory/memory"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":extensions:in-memory:fcc-node-directory-memory"/":services:catalog:extensions:node-directory:memory"/g' {} \;

move "extensions/in-memory/fcc-store-memory" "services/catalog/extensions/federated-catalog-cache-store/memory"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":extensions:in-memory:fcc-store-memory"/":services:catalog:extensions:federated-catalog-cache-store:memory"/g' {} \;

move "extensions/in-memory/identity-hub-memory" "services/catalog/extensions/identity-hub/memory"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":extensions:in-memory:identity-hub-memory"/":services:catalog:extensions:identity-hub:memory"/g' {} \;

move "extensions/in-memory/negotiation-store-memory" "services/connector/extensions/contract/negotiation-store/memory"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":extensions:in-memory:negotiation-store-memory"/":services:connector:extensions:contract:negotiation-store:memory"/g' {} \;

move "extensions/in-memory/policy-registry-memory" "services/connector/extensions/policy/registry/memory"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":extensions:in-memory:policy-registry-memory"/":services:connector:extensions:policy:registry:memory"/g' {} \;

move "extensions/in-memory/transfer-store-memory" "services/connector/extensions/transfer/store/memory"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":extensions:in-memory:transfer-store-memory"/":services:connector:extensions:transfer:store:memory"/g' {} \;

move "extensions/ion/ion-client" "services/catalog/extensions/ion/ion-client"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":extensions:ion:ion-client"/":services:did-registration:extensions:ion:ion-client"/g' {} \;

move "extensions/ion/ion-core" "services/catalog/extensions/ion/ion-core"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":extensions:ion:ion-core"/":services:did-registration:extensions:ion:ion-core"/g' {} \;

move "extensions/ion/build.gradle.kts" "services/catalog/extensions/ion/build.gradle.kts"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":extensions:ion"/":services:did-registration:extensions:ion"/g' {} \;

move "extensions/jdk-logger-monitor" "common/extensions/monitor/console"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":extensions:jdk-logger-monitor"/":common:extensions:monitor:console"/g' {} \;

move "extensions/policy/ids-policy" "services/connector/extensions/contract/policy"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":extensions:policy:ids-policy"/":services:connector:extensions:contract:policy"/g' {} \;

move "extensions/transfer-functions/transfer-functions-core" "services/connector/extensions/transfer/data-flow/http/functions-core"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":extensions:transfer-functions:transfer-functions-core"/":services:connector:extensions:transfer:data-flow:http:functions-core"/g' {} \;

move "extensions/transfer-functions/transfer-functions-spi" "services/connector/extensions/transfer/data-flow/http/functions-spi"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":extensions:transfer-functions:transfer-functions-spi"/":services:connector:extensions:transfer:data-flow:http:functions-spi"/g' {} \;

#delete "extensions"

move "common/util" "common/libraries/util"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":common:util"/":common:libraries:util"/g' {} \;

move "launchers/basic/" "services/connector/launchers/basic"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":launchers:basic"/":services:connector:launchers:basic"/g' {} \;

move "launchers/data-loader-cli" "services/connector/launchers/data-loader-cli"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":launchers:data-loader-cli"/":services:connector:launchers:data-loader-cli"/g' {} \;

move "launchers/ids-connector" "services/connector/launchers/ids-connector"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":launchers:ids-connector"/":services:connector:launchers:ids-connector"/g' {} \;

move "launchers/junit" "common/launchers/junit"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":launchers:junit"/":common:launchers:junit"/g' {} \;

move "launchers/registration-service-app/" "services/did-registration/launchers/registration-service-app"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":launchers:registration-service-app"/":services:did-registration:launchers:registration-service-app"/g' {} \;

#delete "launchers"

move "samples/01-basic-connector" "services/connector/samples/01-basic-connector"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":samples:01-basic-connector"/":services:connector:samples:01-basic-connector"/g' {} \;

move "samples/02-health-endpoint" "services/connector/samples/02-health-endpoint"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":samples:02-health-endpoint"/":services:connector:samples:02-health-endpoint"/g' {} \;

move "samples/03-configuration" "services/connector/samples/03-configuration"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":samples:03-configuration"/":services:connector:samples:03-configuration"/g' {} \;

move "samples/04-file-transfer/api" "services/connector/samples/04-file-transfer/api"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":samples:04-file-transfer:api"/":services:connector:samples:04-file-transfer:api"/g' {} \;

move "samples/04-file-transfer/consumer" "services/connector/samples/04-file-transfer/consumer"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":samples:04-file-transfer:consumer"/":services:connector:samples:04-file-transfer:consumer"/g' {} \;

move "samples/04-file-transfer/provider" "services/connector/samples/04-file-transfer/provider"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":samples:04-file-transfer:provider"/":services:connector:samples:04-file-transfer:provider"/g' {} \;

move "samples/04-file-transfer/file-transfer" "services/connector/samples/04-file-transfer/file-transfer"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":samples:04-file-transfer:file-transfer"/":services:connector:samples:04-file-transfer:file-transfer"/g' {} \;

move "samples/04-file-transfer/README.md" "services/connector/samples/04-file-transfer/README.md"

move "samples/05-file-transfer-cloud/api" "services/connector/samples/05-file-transfer-cloud/api"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":samples:05-file-transfer-cloud:api"/":services:connector:samples:05-file-transfer-cloud:api"/g' {} \;

move "samples/05-file-transfer-cloud/consumer" "services/connector/samples/05-file-transfer-cloud/consumer"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":samples:05-file-transfer-cloud/consumer"/":services:connector:samples:05-file-transfer-cloud/consumer"/g' {} \;

move "samples/05-file-transfer-cloud/data-seeder" "services/connector/samples/05-file-transfer-cloud/data-seeder"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":samples:05-file-transfer-cloud:data-seeder"/":services:connector:samples:05-file-transfer-cloud/data-seeder"/g' {} \;

move "samples/05-file-transfer-cloud/provider" "services/connector/samples/05-file-transfer-cloud/provider"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":samples:05-file-transfer-cloud:provider"/":services:connector:samples:05-file-transfer-cloud:provider"/g' {} \;

move "samples/05-file-transfer-cloud/transfer-file" "services/connector/samples/05-file-transfer-cloud/transfer-file"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":samples:05-file-transfer-cloud:transfer-file"/":services:connector:samples:05-file-transfer-cloud:transfer-file"/g' {} \;

move "samples/05-file-transfer-cloud/README.md" "services/connector/samples/05-file-transfer-cloud/README.md"

move "samples/05-file-transfer-cloud/datarequest.json" "services/connector/samples/05-file-transfer-cloud/datarequest.json"

move "samples/other/commandline/consumer" "common/samples/commandline/consumer"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":samples:other:commandline:consumer"/":common:samples:commandline:consumer"/g' {} \;

move "samples/other/commandline/consumer-runtime" "common/samples/commandline/consumer-runtime"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":samples:other:commandline:consumer-runtime"/":common:samples:commandline:consumer-runtime"/g' {} \;

move "samples/other/copy-between-azure-and-s3" "services/connector/samples/other/copy-between-azure-and-s3"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":samples:other:copy-between-azure-and-s3"/":services:connector:samples:other:copy-between-azure-and-s3"/g' {} \;

move "samples/other/copy-file-to-s3bucket" "services/connector/samples/other/copy-file-to-s3bucket"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":samples:other:copy-file-to-s3bucket"/":services:connector:samples:other:copy-between-azure-and-s3"/g' {} \;

move "samples/other/custom-runtime" "services/connector/samples/other/custom-runtime"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":samples:other:custom-runtime"/":services:connector:samples:other:custom-runtime"/g' {} \;

move "samples/other/dataseed/dataseed-aws" "services/connector/samples/other/dataseed/dataseed-aws"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":samples:other:dataseed:dataseed-aws"/":services:connector:samples:dataseed:dataseed-aws"/g' {} \;

move "samples/other/dataseed/dataseed-azure" "services/connector/samples/other/dataseed/dataseed-azure"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":samples:other:dataseed:dataseed-azure"/":services:connector:samples:dataseed:dataseed-azure"/g' {} \;

move "samples/other/dataseed/dataseed-policy" "services/connector/samples/other/dataseed/dataseed-policy"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":samples:other:dataseed:dataseed-policy"/":services:connector:samples:dataseed:dataseed-policy"/g' {} \;

move "samples/other/file-transfer-s3-to-s3" "services/connector/samples/other/file-transfer-s3-to-s3"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":samples:other:file-transfer-s3-to-s3"/":services:connector:samples:other:file-transfer-s3-to-s3"/g' {} \;

move "samples/other/public-rest-api" "services/connector/samples/other/public-rest-api"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":samples:other:public-rest-api"/":services:connector:samples:other:public-rest-api"/g' {} \;

move "samples/other/run-from-junit" "services/connector/samples/other/run-from-junit"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":samples:other:run-from-junit"/":services:connector:samples:other:run-from-junit"/g' {} \;

move "samples/other/streaming" "services/connector/samples/other/streaming"
find . -name "*.gradle.kts" -type f -exec sed -i '' -e 's/":samples:other:streaming"/":services:connector:samples:other:streaming"/g' {} \;

#delete "samples"

# move "spi" "spi"

# TODO
# - Ask Jim and Paul
#     - Where to move these extensions?
#         /extensions/aws/aws-test
#         /extensions/azure/azure-test
# - Ask everyone
#     - Is there a use case that needs this? Can we delete it?
#         /extensions/azure/events
#         /extensions/in-memory/negotiation/policy-registry-memory
#         /extensions/policy/ids-policy
#         /launchers/basic
# - Fix these packages manually after running the script
#         include(":extensions:aws")
#         include(":extensions:azure:blob")
# - Update package names in a subsequent PR, so that we don't mix moving and updating the files
# - Split '/spi' into the different services in a subsequent PR
