variable "ci_client_id" {
  type        = string
  description = "Application ID (Client ID) of the GitHub workflow that runs the CI job and needs access to cloud resources."
}

variable "prefix" {
  type        = string
  description = "Application name. Must be globally unique. Use only lowercase letters and numbers."
}

variable "location" {
  type        = string
  description = "Azure region where to create resources."
  default     = "North Europe"
}

variable "resource_tags" {
  description = "Tags to set for all resources."
  type        = map(string)
  default     = {}
}
