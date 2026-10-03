variable "repositories" {
  description = "Repository names"
  type        = list(string)
  default     = ["shopsphere-backend", "shopsphere-frontend"]
}

variable "keep_images" {
  description = "How many images to keep per repository"
  type        = number
  default     = 20
}

variable "force_delete" {
  description = "Allow terraform destroy to delete repositories that still contain images (training: true)"
  type        = bool
  default     = true
}

variable "tags" {
  type    = map(string)
  default = {}
}
