-- Database Migration Script for Job Application Management System
-- Run this script on your PostgreSQL database

-- 1. Add interview_details column to jobdetails table
ALTER TABLE jobdetails ADD COLUMN IF NOT EXISTS interview_details TEXT;

-- 2. Add index for better query performance
CREATE INDEX IF NOT EXISTS idx_job_applications_status ON job_applications(status);
CREATE INDEX IF NOT EXISTS idx_job_applications_applicant_email ON job_applications(applicant_email);

-- 3. Verify the changes
SELECT column_name, data_type, character_maximum_length
FROM information_schema.columns
WHERE table_name = 'jobdetails' AND column_name = 'interview_details';

-- Expected output: interview_details | text | NULL

COMMENT ON COLUMN jobdetails.interview_details IS 'JSON string containing interview rounds configuration (Coding, Technical, HR, Project Task)';
