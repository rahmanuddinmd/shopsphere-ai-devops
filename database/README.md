# Database

The schema is owned by **Flyway** migrations inside the backend, so every environment
(local, Docker, Kubernetes, RDS) gets exactly the same schema automatically at startup:

| File | Content |
|---|---|
| `backend/src/main/resources/db/migration/V1__create_schema.sql` | Tables, keys, constraints, indexes |
| `backend/src/main/resources/db/migration/V2__seed_reference_data.sql` | Roles, categories, 8 sample products |

Rules: never edit an applied migration; add `V3__<description>.sql`. Hibernate runs with
`ddl-auto: validate`, so a mismatch between entities and schema fails fast at startup.

Backup / restore (local Compose):
```bash
bash database/backup.sh                                      # writes database/backups/shopsphere-<date>.sql.gz
gunzip -c database/backups/<file>.sql.gz | docker compose exec -T postgres psql -U shopsphere -d shopsphere
```
On RDS use automated backups and snapshots instead (`backup_retention_period` in `terraform/modules/rds`).
