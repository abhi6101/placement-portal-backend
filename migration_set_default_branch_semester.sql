-- Migration script to set default branch and semester for existing students
-- Run this manually on your database

-- Set all existing students (role = 'USER') to IMCA 7th semester
UPDATE users 
SET branch = 'IMCA', semester = 7 
WHERE role = 'USER' AND (branch IS NULL OR semester IS NULL);

-- Verify the update
SELECT username, email, branch, semester FROM users WHERE role = 'USER';
