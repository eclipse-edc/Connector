provider "aws" {
  # Configuration options
  region = var.aws_region
}


resource "aws_iam_user" "hackathon-user" {
  name = "hackathon-user"
  path = "/"
  force_destroy = true
}

resource "aws_iam_access_key" "gx_access_key" {
  user = aws_iam_user.hackathon-user.name

}

resource "aws_iam_user_policy_attachment" "gx-s3fullaccess" {
  user = aws_iam_user.hackathon-user.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonS3FullAccess"
}

resource "aws_iam_user_policy_attachment" "gx-iamfullaccess" {
  user = aws_iam_user.hackathon-user.name
  policy_arn = "arn:aws:iam::aws:policy/IAMFullAccess"
}


resource "aws_s3_bucket" "src-bucket" {
  bucket = "hackathon-src-bucket"
}

output "new_user" {
  sensitive = true
  value = {
    secret = aws_iam_access_key.gx_access_key.secret
    id = aws_iam_access_key.gx_access_key.id
  }
}
