-- Add missing columns for job type and timestamps if they don't exist
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS job_type VARCHAR(255) NOT NULL DEFAULT 'FULL_TIME';
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE jobs ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Note: We're not renaming columns as the database already has both job_description/description and job_title/title
