    -- Database Migration for Smart Password Recovery System
-- Add DOB and Gender fields to users table

-- Add dob column (Date of Birth from Aadhar verification)
ALTER TABLE users ADD COLUMN IF NOT EXISTS dob VARCHAR(20);

-- Add gender column (from Aadhar verification)
ALTER TABLE users ADD COLUMN IF NOT EXISTS gender VARCHAR(10);

-- Add comments for documentation
COMMENT ON COLUMN users.dob IS 'Date of Birth extracted from Aadhar card during verification (Format: DD/MM/YYYY or YYYY-MM-DD)';
COMMENT ON COLUMN users.gender IS 'Gender extracted from Aadhar card during verification (Male/Female)';

-- Optional: Create index for faster queries
CREATE INDEX IF NOT EXISTS idx_users_dob ON users(dob);
CREATE INDEX IF NOT EXISTS idx_users_gender ON users(gender);

-- Verify the changes
SELECT column_name, data_type, character_maximum_length, is_nullable
FROM information_schema.columns
WHERE table_name = 'users' AND column_name IN ('dob', 'gender');
