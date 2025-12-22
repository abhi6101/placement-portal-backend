# 🗄️ Database Migration Guide - Smart Password Recovery System

## ✅ **BUILD STATUS: SUCCESS**

Your backend has been successfully compiled with all the new changes!

```
[INFO] BUILD SUCCESS
[INFO] Total time:  9.832 s
[INFO] Finished at: 2025-12-22T15:15:59+05:30
```

---

## 🔄 **Database Migration - How It Works**

### **Automatic Migration (Recommended)** ✅

Since your `application.properties` has:
```properties
spring.jpa.hibernate.ddl-auto=update
```

**The database migration will happen AUTOMATICALLY** when you deploy/restart your application!

### **What Happens Automatically:**

When you deploy the new backend version, Hibernate will:

1. ✅ Detect the new `dob` field in Users.java
2. ✅ Detect the new `gender` field in Users.java
3. ✅ Execute: `ALTER TABLE users ADD COLUMN dob VARCHAR(20);`
4. ✅ Execute: `ALTER TABLE users ADD COLUMN gender VARCHAR(10);`
5. ✅ Create indexes (if configured)

**No manual SQL execution needed!**

---

## 🚀 **Deployment Steps**

### **Option 1: Deploy to Production (Automatic Migration)**

#### **If using Render/Railway/Heroku:**

1. **Push your code to GitHub:**
   ```bash
   # Already done! ✅
   git push origin main
   ```

2. **Trigger deployment:**
   - Your hosting platform will automatically:
     - Pull the latest code
     - Build the application
     - Run the application
     - **Hibernate will auto-create the new columns**

3. **Verify migration:**
   - Check your database after deployment
   - The `users` table should now have `dob` and `gender` columns

#### **If using manual deployment:**

1. **Build the JAR:**
   ```bash
   .\mvnw.cmd clean package -DskipTests
   ```
   ✅ **Already done! JAR created at:**
   `target/authProject-0.0.1-SNAPSHOT.jar`

2. **Deploy the JAR to your server**

3. **Run the application:**
   ```bash
   java -jar authProject-0.0.1-SNAPSHOT.jar
   ```

4. **Hibernate will automatically create the columns on startup**

---

### **Option 2: Manual Migration (If Needed)**

If you prefer to apply the migration manually before deploying:

#### **Step 1: Connect to your database**

**Using psql (PostgreSQL command line):**
```bash
psql -U your_username -d your_database_name
```

**Or using a GUI tool:**
- pgAdmin
- DBeaver
- DataGrip
- Supabase Dashboard (if using Supabase)

#### **Step 2: Run the migration SQL**

```sql
-- Add dob column
ALTER TABLE users ADD COLUMN IF NOT EXISTS dob VARCHAR(20);

-- Add gender column
ALTER TABLE users ADD COLUMN IF NOT EXISTS gender VARCHAR(10);

-- Add comments for documentation
COMMENT ON COLUMN users.dob IS 'Date of Birth extracted from Aadhar card during verification (Format: DD/MM/YYYY or YYYY-MM-DD)';
COMMENT ON COLUMN users.gender IS 'Gender extracted from Aadhar card during verification (Male/Female)';

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_users_dob ON users(dob);
CREATE INDEX IF NOT EXISTS idx_users_gender ON users(gender);

-- Verify the changes
SELECT column_name, data_type, character_maximum_length, is_nullable
FROM information_schema.columns
WHERE table_name = 'users' AND column_name IN ('dob', 'gender');
```

#### **Step 3: Verify**

You should see output like:
```
 column_name | data_type | character_maximum_length | is_nullable 
-------------+-----------+--------------------------+-------------
 dob         | varchar   |                       20 | YES
 gender      | varchar   |                       10 | YES
```

---

## 🔍 **Verification Checklist**

After deployment, verify the migration worked:

### **1. Check Database Schema**

```sql
-- List all columns in users table
SELECT column_name, data_type, character_maximum_length
FROM information_schema.columns
WHERE table_name = 'users'
ORDER BY ordinal_position;
```

**Expected new columns:**
- ✅ `dob` (varchar, 20)
- ✅ `gender` (varchar, 10)

### **2. Test the Smart Recovery Flow**

#### **Test Route A (Verified User):**
```bash
# 1. Send OTP
curl -X POST https://your-backend.com/api/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{"email":"verified-user@example.com"}'

# 2. Verify OTP (should return SIMPLE_RESET)
curl -X POST https://your-backend.com/api/auth/verify-otp \
  -H "Content-Type: application/json" \
  -d '{"email":"verified-user@example.com","otp":"123456"}'
```

**Expected Response:**
```json
{
  "success": true,
  "route": "SIMPLE_RESET",
  "token": "jwt_token_here",
  "userData": {
    "name": "John Doe",
    "computerCode": "59500",
    "email": "verified-user@example.com"
  }
}
```

#### **Test Route B (Legacy User):**
```bash
# 1. Send OTP
curl -X POST https://your-backend.com/api/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{"email":"legacy-user@example.com"}'

# 2. Verify OTP (should return FULL_VERIFICATION)
curl -X POST https://your-backend.com/api/auth/verify-otp \
  -H "Content-Type: application/json" \
  -d '{"email":"legacy-user@example.com","otp":"123456"}'
```

**Expected Response:**
```json
{
  "success": true,
  "route": "FULL_VERIFICATION",
  "token": "jwt_token_here",
  "userData": {
    "email": "legacy-user@example.com",
    "existingData": {
      "name": "Jane Doe",
      "username": "jane_doe"
    }
  }
}
```

### **3. Check Application Logs**

Look for Hibernate DDL statements in your logs:
```
Hibernate: alter table users add column dob varchar(20)
Hibernate: alter table users add column gender varchar(10)
```

---

## 📊 **Current Deployment Status**

| Component | Status | Location |
|-----------|--------|----------|
| Backend Code | ✅ Complete | GitHub (pushed) |
| Backend Build | ✅ Success | `target/authProject-0.0.1-SNAPSHOT.jar` |
| Database Schema | ⏳ Pending | Will auto-update on deployment |
| Frontend Code | ✅ Complete | GitHub (pushed) |

---

## 🎯 **Next Steps**

### **Immediate Actions:**

1. **Deploy Backend:**
   - Push to your hosting platform (Render/Railway/Heroku)
   - Or manually deploy the JAR file
   - **Migration will happen automatically on first startup**

2. **Deploy Frontend:**
   - Already on Vercel (auto-deploys from GitHub)
   - Or trigger manual deployment

3. **Test the Complete Flow:**
   - Test Route A with a verified user
   - Test Route B with a legacy user
   - Verify emails are sent
   - Check database updates

### **Monitoring:**

After deployment, monitor:
- ✅ Application logs for migration success
- ✅ Database for new columns
- ✅ Email delivery
- ✅ User login with Computer Code
- ✅ Account migration success

---

## ⚠️ **Important Notes**

1. **Backup First:**
   - Always backup your production database before deploying
   - `pg_dump your_database > backup_$(date +%Y%m%d).sql`

2. **Zero Downtime:**
   - Adding columns with `ALTER TABLE ADD COLUMN` is non-blocking
   - Existing functionality will continue to work
   - No data loss risk

3. **Rollback Plan:**
   - If needed, you can remove columns:
     ```sql
     ALTER TABLE users DROP COLUMN dob;
     ALTER TABLE users DROP COLUMN gender;
     ```

4. **Testing:**
   - Test in staging environment first (if available)
   - Verify all flows work before announcing to users

---

## ✅ **Migration Complete Checklist**

Before marking as complete, verify:

- [ ] Backend deployed successfully
- [ ] Application started without errors
- [ ] Database has `dob` and `gender` columns
- [ ] Route A (Simple Reset) works
- [ ] Route B (Full Verification) works
- [ ] Login with Computer Code works
- [ ] Emails are being sent
- [ ] Account migration updates username correctly

---

## 🎉 **You're Ready to Deploy!**

The database migration is **ready to be applied automatically** when you deploy your backend.

**No manual SQL execution needed** - just deploy and Hibernate will handle it!

---

**Last Build:** 2025-12-22T15:15:59+05:30
**Build Status:** ✅ SUCCESS
**JAR Location:** `target/authProject-0.0.1-SNAPSHOT.jar`
