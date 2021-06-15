
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

resource "aws_iam_user_policy_attachment" "dagx-s3fullaccess" {
  user = aws_iam_user.dagx.name
  policy_arn = aws_iam_policy.s3-full-access.arn
}

resource "aws_iam_policy" "s3-full-access" {
  name        = "s3-full-access"
  description = "Allows a user full access to S3"

  policy = <<EOF
{
 "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": "s3:*",
            "Resource": "*"
        }
    ]
}
EOF
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