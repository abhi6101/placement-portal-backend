# Supabase Database Migration - Job Application Management

## 🚀 Quick Migration Steps for Supabase

### Step 1: Access Supabase SQL Editor

1. Go to https://supabase.com/dashboard
2. Select your project
3. Click **"SQL Editor"** in the left sidebar
4. Click **"New query"**

### Step 2: Run Migration SQL

Copy and paste this SQL into the editor:

```sql
-- Add interview_details column to jobdetails table
ALTER TABLE jobdetails ADD COLUMN IF NOT EXISTS interview_details TEXT;

-- Add indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_job_applications_status 
ON job_applications(status);

CREATE INDEX IF NOT EXISTS idx_job_applications_applicant_email 
ON job_applications(applicant_email);

-- Add comment for documentation
COMMENT ON COLUMN jobdetails.interview_details IS 
'JSON string containing interview rounds configuration (Coding, Technical, HR, Project Task)';

-- Verify the column was added
SELECT column_name, data_type, character_maximum_length
FROM information_schema.columns
WHERE table_name = 'jobdetails' AND column_name = 'interview_details';
```

### Step 3: Click "Run" Button

The migration will execute and you should see:
- ✅ Column added successfully
- ✅ Indexes created
- ✅ Verification query shows the new column

---

## ✅ Verification

After running the migration, verify it worked:

```sql
-- Check if column exists
SELECT interview_details FROM jobdetails LIMIT 1;

-- Should return the column (even if NULL for existing rows)
```

---

## 🎯 What This Does

1. **Adds `interview_details` column** - Stores JSON with interview rounds
2. **Creates performance indexes** - Speeds up application queries
3. **Adds documentation** - Comments explain the column purpose

---

## 🔄 Backend Auto-Deploy

Your backend on Render will automatically:
1. Detect the new commit (04323b1)
2. Rebuild with new code
3. Restart with updated EmailService

**No manual restart needed!** ✅

---

## 🧪 Test After Migration

1. **Check Render Logs:**
   - Go to Render Dashboard → Your Backend Service
   - Check logs for successful startup
   - Look for: "Started AuthProjectApplication"

2. **Test Job Posting:**
   - Login to admin dashboard
   - Post a job with interview rounds
   - Should save successfully

3. **Test Email Flow:**
   - Student applies for job
   - Admin accepts → Email sent via SendGrid
   - Check student's email inbox

---

## 📊 Example Interview Details JSON

When you post a job with interview rounds, it saves like this:

```json
{
  "codingRound": {
    "enabled": true,
    "date": "2025-12-20",
    "time": "10:00 AM",
    "venue": "Lab 101",
    "instructions": "Solve 3 DSA problems"
  },
  "technicalInterview": {
    "enabled": true,
    "date": "2025-12-22",
    "time": "2:00 PM",
    "venue": "Virtual - Zoom",
    "topics": "React, Node.js, Databases"
  },
  "hrRound": {
    "enabled": false
  },
  "projectTask": {
    "enabled": false
  }
}
```

---

## ✅ Success Checklist

After migration:
- [ ] SQL ran successfully in Supabase
- [ ] Column `interview_details` exists
- [ ] Indexes created
- [ ] Render backend redeployed
- [ ] No errors in Render logs
- [ ] Admin can post jobs with interview rounds
- [ ] Emails are sent when applications are accepted/rejected

---

## 🎉 You're Done!

Once the migration runs:
- ✅ Database schema updated
- ✅ Backend code deployed
- ✅ Frontend already live
- ✅ Complete workflow ready to use

**Your placement portal is production-ready!** 🚀

---

## 🆘 Troubleshooting

**Issue: Column already exists**
- Solution: The migration uses `IF NOT EXISTS`, safe to run multiple times

**Issue: Permission denied**
- Solution: Make sure you're logged into the correct Supabase project

**Issue: Table not found**
- Solution: Verify table name is `jobdetails` (lowercase)
- Check with: `SELECT * FROM jobdetails LIMIT 1;`
