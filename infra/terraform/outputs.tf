output "ecr_repository_url" {
  description = "Repository address used for Docker image pushes"
  value       = aws_ecr_repository.studentapp.repository_url
}

output "ecr_repository_arn" {
  description = "Repository ARN used when configuring IAM permissions"
  value       = aws_ecr_repository.studentapp.arn
}
