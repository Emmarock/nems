-- "Estate Levy" and "Waste Levy" were placeholder seed-data levy categories (V4) that don't
-- apply to Nitel Estate at all — confirmed unused (zero invoices ever raised against either) and
-- removed from the live dev catalog. This makes the removal reproducible for every environment,
-- including fresh ones and ephemeral test databases seeded straight from V4.

delete from levy where name in ('Estate Levy', 'Waste Levy');
