output "primary_client_id" {
  value = azuread_application.demo-app-id.application_id
}

output "primary_id_certfile" {
  value = abspath("${path.root}/cert.pfx")
}

//output "URLs" {
//  value = {
//    provider    = "https://${module.provider-cluster-bmw.public-ip.fqdn}"
//    consumer-fr = azurerm_container_group.consumer-fr.fqdn
//  }
//}

//output "namespaces" {
//  value = {
//    connector = module.provider-bmw-deployment.connector-cluster-namespace
//  }
//}
