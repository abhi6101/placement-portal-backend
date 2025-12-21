# Fix IMCA Course Duration - Manual Database Update

## Problem
IMCA (Integrated Master of Computer Applications) is showing as a 4-year course (8 semesters) but it's actually a 5-year integrated program (10 semesters).

This causes the batch calculation to show:
- **Current (Wrong):** 2022-2026 (4 years)
- **Should Be:** 2022-2027 (5 years)

---

## Solution 1: Run SQL Directly on Database

### Option A: Using psql (PostgreSQL Command Line)

```bash
# Connect to your database
psql -h <your-database-host> -U <username> -d <database-name>

# Run the update
UPDATE department_branches 
SET max_semesters = 10 
WHERE branch_code = 'IMCA';

# Verify the change
SELECT branch_code, branch_name, max_semesters, degree 
FROM department_branches 
WHERE branch_code IN ('IMCA', 'MCA', 'BCA')
ORDER BY branch_code;

# Exit
\q
```

### Option B: Using Database GUI (pgAdmin, DBeaver, etc.)

1. Connect to your database
2. Open SQL Query window
3. Run this SQL:

```sql
UPDATE department_branches 
SET max_semesters = 10 
WHERE branch_code = 'IMCA';
```

4. Verify:

```sql
SELECT branch_code, branch_name, max_semesters, degree 
FROM department_branches 
WHERE branch_code IN ('IMCA', 'MCA', 'BCA');
```

Expected result:
```
branch_code | branch_name                           | max_semesters | degree
------------|---------------------------------------|---------------|--------
BCA         | Bachelor of Computer Applications     | 6             | BCA
IMCA        | Integrated Master of Computer Apps    | 10            | IMCA
MCA         | Master of Computer Applications       | 4             | MCA
```

---

## Solution 2: Using Flyway Migration (Recommended for Production)

The migration file has been created at:
```
src/main/resources/db/migration/fix_imca_duration.sql
```

**Steps:**
1. Commit the migration file to git
2. Deploy the backend
3. Flyway will automatically run the migration on startup

---

## Solution 3: Using Backend API (If you have admin endpoints)

If your backend has an admin API to update departments:

```bash
curl -X PUT http://localhost:8080/api/admin/departments/branches/IMCA \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
  -d '{"maxSemesters": 10}'
```

---

## Verification

After running the update, test on the frontend:

1. Go to `/register`
2. Complete verification for an IMCA student
3. Check the **Batch Session** field
4. For admission year 2022, it should show: **2022-2027** ✅

---

## Database Connection Info

Check your `.env` file or environment variables for:
- `DATABASE_URL`
- `DATABASE_USERNAME`
- `DATABASE_PASSWORD`

Example for Railway/Render:
```
DATABASE_URL=postgresql://user:password@host:5432/database
```

---

## Quick Fix for Testing (Temporary)

If you can't access the database right now, you can temporarily hardcode it in the frontend:

**In Register.jsx, find the batch calculation and add:**

```javascript
const calculateBatch = (branch, startYear) => {
    const durationMap = {
        'IMCA': 5,  // 5 years (10 semesters)
        'MCA': 2,   // 2 years (4 semesters)
        'BCA': 3,   // 3 years (6 semesters)
    };
    
    const duration = durationMap[branch] || 4;
    return `${startYear}-${parseInt(startYear) + duration}`;
};
```

But **this is temporary** - the proper fix is updating the database!

---

## After Fix

Once the database is updated:
1. Restart the backend server
2. Clear browser cache
3. Test registration for IMCA student
4. Batch should show correctly: 2022-2027 ✅
