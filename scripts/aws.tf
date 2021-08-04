provider "aws" {
  # Configuration options
  region = var.aws_region
}


resource "aws_iam_user" "edc-user" {
  name = "edc"
  path = "/"
  force_destroy = true
}

resource "aws_iam_access_key" "edc_access_key" {
  user = aws_iam_user.edc-user.name

}

resource "aws_iam_user_policy_attachment" "edc-s3fullaccess" {
  user = aws_iam_user.edc-user.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonS3FullAccess"
}

resource "aws_iam_user_policy_attachment" "edc-iamfullaccess" {
  user = aws_iam_user.edc-user.name
  policy_arn = "arn:aws:iam::aws:policy/IAMFullAccess"
}


resource "aws_s3_bucket" "src-bucket" {
  bucket = "edc-src-bucket"
}

output "new_user" {
  sensitive = true
  value = {
    secret = aws_iam_access_key.edc_access_key.secret
    id = aws_iam_access_key.edc_access_key.id
  }
}