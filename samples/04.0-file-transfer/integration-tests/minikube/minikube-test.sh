#!/bin/bash

# CI script for testing Helm chart with data transfer sample on minikube.
#
# See README.md for preconditions for running this script manually.

set -euxo pipefail

dir=$(dirname $0)

# Build and install Consumer and Provider connectors

for target in consumer provider; do
  docker build -t $target --build-arg JAR=samples/04.0-file-transfer/$target/build/libs/$target.jar -f launchers/generic/Dockerfile .
  helm upgrade --install -f $dir/values-$target.yaml $target resources/charts/dataspace-connector
done

# Wait for pods to be live

for target in consumer provider; do
  kubectl wait --for=condition=available deployment $target-dataspace-connector --timeout=120s
done

# Resolve service address for Consumer

export CONSUMER_URL=$(minikube service --url consumer-dataspace-connector)

# Perform negotiation and file transfer. See sample root directory README.md file for more details.

export PROVIDER_URL="http://provider-dataspace-connector"
export DESTINATION_PATH="/tmp/destination-file-$RANDOM"
export API_KEY="password"
export RUN_INTEGRATION_TEST="true"

./gradlew :samples:04.0-file-transfer:integration-tests:test --tests org.eclipse.dataspaceconnector.samples.FileTransferAsClientIntegrationTest

kubectl exec deployment/provider-dataspace-connector -- wc -l $DESTINATION_PATH
echo "Test succeeded."
