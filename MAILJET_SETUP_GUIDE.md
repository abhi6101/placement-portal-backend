# Mailjet Email Service Setup Guide

## 🚀 Quick Start - Mailjet Integration

### Step 1: Create Mailjet Account (FREE)

1. **Go to**: https://www.mailjet.com/
2. **Click**: "Sign Up Free"
3. **Fill in**:
   - Email address
   - Password
   - Company name (can be anything, e.g., "Hack2Hired")
4. **Verify** your email address
5. **No credit card required!**

---

### Step 2: Get Your API Credentials

1. **Login** to Mailjet dashboard
2. **Go to**: Account Settings → API Keys (or https://app.mailjet.com/account/apikeys)
3. **You'll see**:
   - **API Key** (like: `a1b2c3d4e5f6g7h8i9j0`)
   - **Secret Key** (like: `z9y8x7w6v5u4t3s2r1q0`)
4. **Copy both** - you'll need them for environment variables

---

### Step 3: Verify Sender Email

1. **Go to**: Account Settings → Sender Addresses
2. **Click**: "Add a Sender Address"
3. **Enter**: `hack2hired.official@gmail.com` (or your email)
4. **Verify** the email by clicking the link sent to your inbox
5. **Wait** for verification (usually instant)

---

### Step 4: Set Environment Variables in Render

Go to your Render dashboard → Your Backend Service → Environment

**Add these variables**:

```bash
MAILJET_API_KEY=your_api_key_here
MAILJET_SECRET_KEY=your_secret_key_here
MAILJET_FROM_EMAIL=hack2hired.official@gmail.com
MAILJET_FROM_NAME=Hack2Hired Team
```

**Example**:
```
MAILJET_API_KEY=a1b2c3d4e5f6g7h8i9j0
MAILJET_SECRET_KEY=z9y8x7w6v5u4t3s2r1q0
MAILJET_FROM_EMAIL=hack2hired.official@gmail.com
MAILJET_FROM_NAME=Hack2Hired Team
```

---

### Step 5: Keep Existing Variables

**Don't delete these** (needed for other features):
```bash
DATABASE_URL=your_database_url
DATABASE_USERNAME=your_db_username
DATABASE_PASSWORD=your_db_password
JWT_SECRET=your_jwt_secret
APPLICATION_RECIPIENT_EMAIL=hack2hired.official@gmail.com
CLOUDINARY_CLOUD_NAME=your_cloudinary_name
CLOUDINARY_API_KEY=your_cloudinary_key
CLOUDINARY_API_SECRET=your_cloudinary_secret
FRONTEND_URL=https://hack-2-hired.onrender.com
BACKEND_URL=https://placement-portal-backend-nwaj.onrender.com
```

---

## 📊 Mailjet Free Tier Limits

- ✅ **6,000 emails per month** - FREE FOREVER
- ✅ **200 emails per day**
- ✅ **No credit card required**
- ✅ **Unlimited contacts**
- ✅ **Email tracking & analytics**

---

## 🔧 Local Development (.env file)

Create/update `.env` file in your project root:

```properties
MAILJET_API_KEY=your_api_key_here
MAILJET_SECRET_KEY=your_secret_key_here
MAILJET_FROM_EMAIL=hack2hired.official@gmail.com
MAILJET_FROM_NAME=Hack2Hired Team
```

---

## ✅ Testing Your Setup

After deployment, test by:
1. Registering a new user (should send verification email)
2. Resetting password (should send OTP email)
3. Applying for a job (should send confirmation email)

---

## 🆚 Mailjet vs SendGrid

| Feature | Mailjet (Free) | SendGrid (Free) |
|---------|----------------|-----------------|
| Monthly Emails | **6,000** | 3,000 |
| Daily Emails | **200** | 100 |
| Credit Card | ❌ Not Required | ✅ Required |
| Attachments | ✅ Yes | ✅ Yes |
| HTML Emails | ✅ Yes | ✅ Yes |

---

## 🔗 Useful Links

- **Dashboard**: https://app.mailjet.com/
- **API Docs**: https://dev.mailjet.com/
- **Java SDK**: https://github.com/mailjet/mailjet-apiv3-java
- **Support**: https://www.mailjet.com/support/

---

## 🐛 Troubleshooting

### Email not sending?
1. Check API credentials are correct
2. Verify sender email is verified in Mailjet
3. Check Render logs for error messages
4. Ensure you haven't exceeded daily limit (200 emails)

### "401 Unauthorized" error?
- API Key or Secret Key is wrong
- Check environment variables in Render

### "403 Forbidden" error?
- Sender email not verified
- Go to Mailjet dashboard and verify your email

---

## 📞 Need Help?

If you encounter issues:
1. Check Render deployment logs
2. Verify all environment variables are set
3. Test with a simple email first
4. Check Mailjet dashboard for delivery status
