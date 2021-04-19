# output "client_certificate" {
#   value = data.azurerm_kubernetes_cluster.default.kube_config.0.client_certificate
# }

# output "kube_config" {
#   value     = data.azurerm_kubernetes_cluster.default.kube_config_raw
#   sensitive = true
# }

# output "nifi_client_secret" {
#   value     = azuread_application_password.dagx-terraform-nifi-app-secret.value
#   sensitive = true
# }

# output "nifi_client_id" {
#   value = azuread_application.dagx-terraform-nifi-app.application_id
# }

output "kubeconfig_path" {
  value = abspath("${path.root}/kubeconfig")
}

output "cluster_name" {
  value = local.cluster_name
}