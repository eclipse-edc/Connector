# Helm Chart for Apache Nifi

## Introduction

This [Helm](https://github.com/kubernetes/helm) chart installs [nifi](https://nifi.apache.org/) in a Kubernetes cluster.

## Prerequisites

- Kubernetes cluster 1.10+
- Helm 3.1.2+(works with helm v2 also, 2.8.0+)
- PV provisioner support in the underlying infrastructure.


## Installation

### Install the chart

Get the zookeeper chart from official [helm-charts](https://github.com/helm/charts), zookeeper is part of incubated [charts](https://github.com/helm/charts/tree/master/incubator/zookeeper).
We will add incubator charts' repository and then install nifi chart.

```bash
$ cd nifi-builder/helm-charts/nifi
# Add helm charts incubator repo
$ helm repo add incubator https://charts.helm.sh/incubator
$ helm repo update
# Update charts dependencies
$ helm dep up
# Create nifi namespace
$ kubectl create namespace nifi
# Install helm chart as release named cluster1 in namespace nifi
$ helm install cluster1 . -n nifi
```

## Uninstallation

To uninstall/delete the `cluster1` deployment:

```bash
$ helm delete cluster1
```

# Installing chart on minikube

Due to lack of resources on a minikube installation and other differences like those of volume storageclass, there is a separate [values-file](./minikube-values.yaml) for minikube one can use.

- Install Apache Nifi using the chart on minikube, use [minikube-values.yaml](./minikube-values.yaml):

    ```bash
    $ kubectl create namespace nifi
    $ helm install cluster1 -f minikube-values.yaml . -n nifi
    ```
- Install secured Apache Nifi using the self-signed certificate generated using nifi-toolkit, use [secured-values-with-nifi-toolkit.yaml](./secured-values-with-nifi-toolkit.yaml):

    ```bash
    $ kubectl create namespace nifi
    $ helm install cluster1 -f minikube-values.yaml -f secured-values-with-nifi-toolkit.yaml . -n nifi
    ```

- Increase number of nifi cluster members on a precreated cluster, we can create a cluster using one of the ways suggested above.

    ```bash
    # Get the stateful set name from the deployed cluster
    $ kubectl -n nifi get sts
    NAME                 READY   AGE
    cluster1-nifi        0/1     24s
    cluster1-zookeeper   0/1     24s

    # Scale up stateful set to increase nifi cluster members
    $ kubectl -n nifi scale sts cluster1-nifi  --replicas=4

    # Similarly, one can scale down stateful set to decrease nifi cluster members
    $ kubectl -n nifi scale sts cluster1-nifi  --replicas=2
    ```

# Checking nifi logs for the pods

All the pods have separate containers running which show the nifi logs (bootstrap, app and user logs).

One would need to get details of the pod they want to check as follows and then can check the logs of specific container.

```bash
$ kubectl get pods -n nifi
NAME                   READY   STATUS    RESTARTS   AGE
cluster1-nifi-0        4/4     Running   0          16m
cluster1-zookeeper-0   1/1     Running   0          16m

# Pod named cluster1-nifi-0 is the one running nifi
# To check bootstrap logs
$ kubectl -n nifi logs cluster1-nifi-0 -c bootstrap-log
# Use -f to follow the logs
$ kubectl -n nifi logs -f cluster1-nifi-0 -c bootstrap-log

# To check app logs
$ kubectl -n nifi logs cluster1-nifi-0 -c app-log
# Use -f to follow the logs
$ kubectl -n nifi logs -f cluster1-nifi-0 -c app-log

# To check user logs
$ kubectl -n nifi logs cluster1-nifi-0 -c user-log
# Use -f to follow the logs
$ kubectl -n nifi logs -f cluster1-nifi-0 -c user-log
```
