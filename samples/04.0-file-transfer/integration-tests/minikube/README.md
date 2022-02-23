# File transfer system tests

This system test serves to validate both the connector runtime and the EDC Helm chart with an end-to-end flow.
It runs within GitHub Actions.

To run it locally, install:
- [Minikube and Kubectl](https://kubernetes.io/docs/tasks/tools/)
- [Helm](https://helm.sh/docs/intro/install/)
- [Docker](https://docs.docker.com/get-docker/)

## Install helm release with minikube

Start minikube:

```bash
minikube start
```

[Set minikube docker environment](https://minikube.sigs.k8s.io/docs/handbook/pushing/#1-pushing-directly-to-the-in-cluster-docker-daemon-docker-env).

```bash
eval $(minikube docker-env)
```

The first step to running this sample is building and starting both the provider and the consumer connector. This is
done the same way as in the previous samples.

Build EDC consumer and provider runtime JAR files:

```bash
./gradlew samples:04.0-file-transfer:consumer:build
./gradlew samples:04.0-file-transfer:provider:build
````

Run tests:

```bash
samples/04.0-file-transfer/integration-tests/minikube/minikube-test.sh
```

Note: on certain Mac platforms, the `minikube service` commands run within the script hangs during execution. If that happens consider using a different minikube driver, such as Parallels or VirtualBox.
