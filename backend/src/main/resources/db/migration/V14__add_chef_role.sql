-- The kitchen screen runs as its own actor: a chef reads the SENT cook queue
-- and marks rounds READY, and can reach nothing else. Until now that ready
-- action lived only with the cashier (see V1, which seeded ADMIN and CASHIER).
--
-- ON CONFLICT guards a re-run against roles.name's UNIQUE constraint; existing
-- accounts are untouched, so nobody gains kitchen access from this migration.
INSERT INTO roles (id, name, created_at, updated_at)
VALUES (gen_random_uuid(), 'CHEF', now(), now())
ON CONFLICT (name) DO NOTHING;
