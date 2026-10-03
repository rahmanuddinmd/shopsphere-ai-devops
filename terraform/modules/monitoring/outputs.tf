output "budget_name" {
  value = length(aws_budgets_budget.monthly) > 0 ? aws_budgets_budget.monthly[0].name : null
}
