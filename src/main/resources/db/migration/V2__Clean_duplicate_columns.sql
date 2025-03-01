-- This migration script removes unused duplicate columns
-- It's safe to execute because we've confirmed all these columns contain NULL values

-- Remove unused duplicate columns
ALTER TABLE jobs DROP COLUMN IF EXISTS job_description;
ALTER TABLE jobs DROP COLUMN IF EXISTS job_title;
ALTER TABLE jobs DROP COLUMN IF EXISTS job_owner;
ALTER TABLE jobs DROP COLUMN IF EXISTS job_owner_id;

-- Log the cleanup action
SELECT 'Removed unused duplicate columns from jobs table' AS message;
