output "primary_client_id" {
  value = azuread_application.dagx-terraform-app.application_id
}

output "primary_id_certfile" {
  value = abspath("${path.root}/cert.pfx")
}

output "URLs" {
  value = {
    nifi  = "https://${module.nifi-cluster.public-ip.fqdn}"
    atlas = "https://${module.atlas-cluster.public-ip.fqdn}"
  }
}

output "namespaces" {
  value = {
    nifi  = module.nifi-deployment.nifi-cluster-namespace
    atlas = module.atlas-deployment.atlas-cluster-namespace
  }
}
