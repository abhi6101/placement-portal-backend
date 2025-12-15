# Render Deployment Guide - Job Application Management System

## 🚀 Quick Deployment Steps

### Step 1: Backend Auto-Deploy (Already Done ✅)
Your backend changes are already pushed to GitHub (commit 04323b1).
Render will automatically detect and deploy the new version.

### Step 2: Run Database Migration on Render

**Option A: Using Render Dashboard (Recommended)**

1. Go to https://dashboard.render.com
2. Select your PostgreSQL database
3. Click "Connect" → "External Connection"
4. Copy the PSQL Command (looks like):
   ```
   PGPASSWORD=xxx psql -h dpg-xxx.oregon-postgres.render.com -U placement_portal_user placement_portal
   ```
5. Run it in your terminal
6. Once connected, run:
   ```sql
   ALTER TABLE jobdetails ADD COLUMN IF NOT EXISTS interview_details TEXT;
   CREATE INDEX IF NOT EXISTS idx_job_applications_status ON job_applications(status);
   CREATE INDEX IF NOT EXISTS idx_job_applications_applicant_email ON job_applications(applicant_email);
   ```

**Option B: Using Render Shell**

1. Go to your backend service on Render
2. Click "Shell" tab
3. Run:
   ```bash
   psql $DATABASE_URL -c "ALTER TABLE jobdetails ADD COLUMN IF NOT EXISTS interview_details TEXT;"
   ```

### Step 3: Verify Deployment

1. **Check Backend Logs:**
   - Go to Render Dashboard → Your Backend Service → Logs
   - Look for successful startup messages
   - Verify no errors related to database schema

2. **Test API Endpoints:**
   ```bash
   # Test if backend is running
   curl https://placement-portal-backend-nwaj.onrender.com/api/health
   
   # Test job applications endpoint
   curl https://placement-portal-backend-nwaj.onrender.com/api/job-applications/my \
     -H "Authorization: Bearer YOUR_TOKEN"
   ```

3. **Frontend Already Deployed:**
   - Your frontend is already live at: https://hack-2-hired.onrender.com
   - It will automatically connect to the updated backend

### Step 4: Test Complete Workflow

1. **Admin Posts Job:**
   - Go to https://hack-2-hired.onrender.com/admin
   - Post a job with interview rounds enabled
   - Verify it saves successfully

2. **Student Applies:**
   - Student applies for the job
   - Application appears in admin dashboard

3. **Admin Accepts:**
   - Click "Accept" button
   - Check student's email for acceptance message
   - Verify interview details are included

4. **Student Sees Update:**
   - Student dashboard shows SHORTLISTED status
   - Green notification banner appears
   - Status badge is green with checkmark

---

## 🔧 Environment Variables (Already Set ✅)

Your Render backend should have these environment variables:
- `SENDGRID_API_KEY` - ✅ Already configured
- `DATABASE_URL` - ✅ Auto-configured by Render
- `sendgrid.from.email` - ✅ Set to hack2hired.official@gmail.com

---

## 📊 What Changed in This Deployment

### Backend Changes:
1. Added `SHORTLISTED` status to ApplicationStatus enum
2. Added `interview_details` column to JobDetails entity
3. Enhanced EmailService with acceptance/rejection emails
4. Updated JobApplicationService to send emails on status change
5. Added GET `/job-applications/my` endpoint for students
6. Added `findByApplicantEmail()` repository method

### Frontend Changes (Already Deployed):
1. Interview rounds form in job posting
2. Job applications manager in admin dashboard
3. Student dashboard with application status
4. Email service integration
5. Notification banners

---

## ✅ Verification Checklist

After migration runs:

- [ ] Backend deployed successfully on Render
- [ ] Database migration completed (interview_details column exists)
- [ ] No errors in Render logs
- [ ] Admin can post jobs with interview rounds
- [ ] Students can apply for jobs
- [ ] Admin can accept/reject applications
- [ ] Emails are sent (check SendGrid dashboard)
- [ ] Student sees status updates in dashboard
- [ ] Notification banner appears for shortlisted students

---

## 🆘 Troubleshooting

### Issue: Column already exists error
**Solution:** The migration uses `IF NOT EXISTS`, so it's safe to run multiple times

### Issue: Emails not sending
**Solution:** 
1. Check SendGrid dashboard for delivery status
2. Verify SENDGRID_API_KEY is set in Render environment variables
3. Check Render logs for email-related errors

### Issue: Backend won't start
**Solution:**
1. Check Render logs for specific error
2. Verify all dependencies in pom.xml
3. Ensure DATABASE_URL is set correctly

---

## 🎉 Success!

Once the migration runs and backend redeploys:
- ✅ Complete job application workflow is live
- ✅ Automated emails working
- ✅ Students can track application status
- ✅ Admins can manage applications efficiently

**Your placement portal is now production-ready!** 🚀
