-- Fix IMCA course duration from 4 years (8 semesters) to 5 years (10 semesters)
-- IMCA (Integrated MCA) is a 5-year program, not 4 years

UPDATE department_branches 
SET max_semesters = 10 
WHERE branch_code = 'IMCA';

-- Verify the update
SELECT branch_code, branch_name, max_semesters, degree 
FROM department_branches 
WHERE branch_code IN ('IMCA', 'MCA', 'BCA')
ORDER BY branch_code;
