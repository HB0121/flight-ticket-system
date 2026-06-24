# Task Report: Append `favorite` table to init.sql

## Status: DONE

## What was changed
Appended the `favorite` table definition to `infra/mysql/init.sql` (lines 108-117). The new table has:
- Primary key `id` (auto-increment)
- Columns: `user_id`, `flight_id`, `created_at`
- Unique constraint on `(user_id, flight_id)`
- Foreign keys referencing `app_user(id)` and `flight(id)`
- Index on `(user_id, created_at)`

## Concerns
None. The table uses `if not exists` so it is safe for repeated executions of the init script.
