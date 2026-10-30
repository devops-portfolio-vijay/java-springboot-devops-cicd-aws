provider "aws" {
  region = "us-east-1"

  allowed_account_ids = ["152439496947"]

  default_tags {
    tags = {
      Project     = "studentapp-devsecops"
      Environment = "learning"
      ManagedBy   = "Terraform"
    }
  }
}
