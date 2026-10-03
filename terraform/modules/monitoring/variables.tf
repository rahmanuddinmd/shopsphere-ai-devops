variable "name" {
  type = string
}

variable "monthly_budget_usd" {
  type    = number
  default = 15
}

variable "budget_emails" {
  description = "Emails that receive budget alerts (empty list = no budget created)"
  type        = list(string)
  default     = []
}
