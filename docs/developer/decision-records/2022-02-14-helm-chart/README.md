# Helm chart

## Decision

A [Helm Chart](https://helm.sh) is provided to be used to deploy the EDC runtime in Kubernetes environments. The chart is highly configurable:

- Configurable runtime based on Docker image of base EDC, Data Plane Framework Server, or custom runtime.
- Configurable environment variables (as [ConfigMap](https://kubernetes.io/docs/concepts/configuration/configmap/)) for runtime settings, following [12-factor principles](https://12factor.net/config).
- Configurable secrets (injected as environment variables into pods).
- Configurable labels and annotations.

- Configurable ingress.

The chart was generated using `helm create` in order to follow best practice for components, configurability and ingress definitions, and adapted to fit the EDC runtime.

The chart must be tested in CI like any other code. A GitHub worklow installs minikube (a local lightweight kubernetes installation) and deploys two releases of the chart, for the Consumer and Provider EDC connector of sample 04.0. The workflow then triggers file copy and verifies that the destination file was created on the Provider pod.
