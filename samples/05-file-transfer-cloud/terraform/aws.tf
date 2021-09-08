provider "aws" {
  # Configuration options
  region = var.aws_region
}


resource "aws_iam_user" "user" {
  name          = "${var.environment}-aws-user"
  path          = "/"
  force_destroy = true
}

resource "aws_iam_access_key" "access_key" {
  user = aws_iam_user.user.name

}

resource "aws_iam_user_policy_attachment" "s3fullaccess" {
  user       = aws_iam_user.user.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonS3FullAccess"
}

resource "aws_iam_user_policy_attachment" "iamfullaccess" {
  user       = aws_iam_user.user.name
  policy_arn = "arn:aws:iam::aws:policy/IAMFullAccess"
}


resource "aws_s3_bucket" "src-bucket" {
  bucket = "${var.environment}-src-bucket"
}

output "new_user" {
  sensitive = true
  value = {
    secret = aws_iam_access_key.access_key.secret
    id     = aws_iam_access_key.access_key.id
  }
}
