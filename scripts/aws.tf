
provider "aws" {
  # Configuration options
  region = var.aws_region
}


resource "aws_iam_user" "dagx" {
  name          = "dagx"
  path          = "/"
  force_destroy = true
}

resource "aws_iam_access_key" "dagx_access_key" {
  user = aws_iam_user.dagx.name

}

resource "aws_iam_user_policy_attachment" "dagx-s3fullaccess" {
  user       = aws_iam_user.dagx.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonS3FullAccess"
}

resource "aws_iam_user_policy_attachment" "dagx-iamfullaccess" {
  user       = aws_iam_user.dagx.name
  policy_arn = "arn:aws:iam::aws:policy/IAMFullAccess"
}


resource "aws_s3_bucket" "src-bucket" {
  bucket = "dagx-src-bucket"
}

output "new_user" {
  sensitive = true
  value = {
    secret = aws_iam_access_key.dagx_access_key.secret
    id     = aws_iam_access_key.dagx_access_key.id
  }
}