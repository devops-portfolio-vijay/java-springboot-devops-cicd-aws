resource "aws_ecr_repository" "studentapp" {
  name = "studentapp-devsecops"

  image_tag_mutability = "IMMUTABLE"
  force_delete         = false

  encryption_configuration {
    encryption_type = "AES256"
  }
}
