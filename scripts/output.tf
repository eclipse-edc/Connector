output "primary_client_id" {
  value = azuread_application.edc-primary-app.application_id
}

output "primary_id_certfile" {
  value = abspath("${path.root}/cert.pfx")
}

output "URLs" {
  value = {
    connector = "https://${module.connector-cluster.public-ip.fqdn}"
  }
}
