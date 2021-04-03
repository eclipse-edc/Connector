
# atlas-helm-chart

This is a Kubernetes Helm Chart that Deploys the Apache Atlas Cluster on Azure Kubernetes Service. Atlas will use Solr for indexing and Cassandra as Backend Storage.

The original Github repo: https://github.com/manjitsin/atlas-helm-chart

# PreRequisites

This Post assumes that you have an already running Kubernetes Cluster in Azure Kubernetes Service. This Chart will also work in other environment as well. Also make sure to install Helm on top of the cluster.

# Helm Chart Deployment

This Helm chart is referenced from another Github Repo: https://github.com/xmavrck/atlas-helm-chart

Let us go through the Folder Structure of the Helm Chart. The charts folder contains solr, atlas, Cassandra and zookeeper Helm Charts embedded. The Atlas Chart will deploy these charts. To deploy the helm chart, clone the repo and run the below command-

```sh
helm install --name <release name>  atlas-helm-chart

Sample Example: helm install --name atlas atlas-helm-chart

Or omit the release name

helm install -g atlas-helm-chart

This will run the solr, atlas, Cassandra and zookeeper pods

solr version : 7.7.2

atlas version : 2.1.1

cassandra version : 3.11.3

zookeeper version : 3.5.5
```

The Helm Chart for Cassandra, Zookeeper and Solr is taken from Helm Stable Chart repository.
