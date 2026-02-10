# 🚀 Mailjet Quick Start - Environment Variables

## Copy-Paste This to Render

Go to: **Render Dashboard → Your Backend Service → Environment**

### ✅ ADD These Variables:

```bash
MAILJET_API_KEY=your_mailjet_api_key_here
MAILJET_SECRET_KEY=your_mailjet_secret_key_here
MAILJET_FROM_EMAIL=hack2hired.official@gmail.com
MAILJET_FROM_NAME=Hack2Hired Team
```

### ❌ DELETE These Variables:

```bash
SENDGRID_API_KEY
SENDER_FROM_EMAIL
```

### ✅ KEEP These Variables (Don't Touch):

```bash
DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD
JWT_SECRET
APPLICATION_RECIPIENT_EMAIL
CLOUDINARY_CLOUD_NAME
CLOUDINARY_API_KEY
CLOUDINARY_API_SECRET
FRONTEND_URL
BACKEND_URL
```

---

## 📧 Where to Get Mailjet Credentials?

1. **Sign up**: https://www.mailjet.com/ (FREE, no credit card)
2. **Get API Keys**: Dashboard → Account Settings → API Keys
3. **Verify Email**: Dashboard → Account Settings → Sender Addresses

---

## 🎯 That's It!

After setting environment variables:
1. Render will auto-deploy
2. Check logs for any errors
3. Test by sending a password reset email

**You now have 6,000 FREE emails per month!** 🎉
