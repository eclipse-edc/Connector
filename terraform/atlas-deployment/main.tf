terraform {
  required_providers {
    kubernetes = {
      source = "hashicorp/kubernetes"
      version = ">= 2.0.3"
    }
    helm = {
      source = "hashicorp/helm"
      version = ">= 2.1.0"
    }
  }
}

resource "kubernetes_namespace" "atlas" {
  metadata {
    name = var.kubernetes-namespace
    labels = {
      "environment" = "test"
    }
  }
}

resource "helm_release" "atlas" {
  name = "dagx-atlas-release"
  chart = var.chart-dir
  values = []
  namespace = kubernetes_namespace.atlas.metadata[0].name
  cleanup_on_fail = true
  set {
    name = "ingress.enabled"
    value = true
  }
  set {
    name = "ingress.hosts"
    value = "dagx-atlas-${var.resourcesuffix}.${var.location}.cloudapp.azure.com"
  }
}

resource "local_file" "kubeconfig" {
  content = var.kubeconfig
  filename = "${path.root}/kubeconfig"
}
