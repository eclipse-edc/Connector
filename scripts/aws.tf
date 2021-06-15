
provider "aws" {
  # Configuration options
  region = var.aws_region
}


resource "aws_iam_user" "dagx" {
  name = "dagx"
  path = "/"
  force_destroy = true
}

resource "aws_iam_access_key" "dagx_access_key" {
  user = aws_iam_user.dagx.name

}

output "new_user" {
  sensitive = true
  value = {
    secret = aws_iam_access_key.dagx_access_key.secret
    id     = aws_iam_access_key.dagx_access_key.id
  }
}