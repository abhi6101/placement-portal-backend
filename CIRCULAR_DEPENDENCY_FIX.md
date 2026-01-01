# 🔧 Circular Dependency Fix - Spring Boot Backend

## 🐛 Problem
Your Spring Boot application was failing to start with the error:
```
Circular placeholder reference 'FRONTEND_URL:https://hack-2-hired.vercel.app' in property definitions
```

## 🔍 Root Cause
In `application.properties`, the property was defined as:
```properties
FRONTEND_URL=${FRONTEND_URL:https://hack-2-hired.vercel.app}
```

This creates a **circular reference** because `FRONTEND_URL` is trying to reference itself, causing Spring to enter an infinite loop when resolving the placeholder.

## ✅ Solution Applied

### 1. **Updated `application.properties`**
Changed property names to avoid self-reference:

**Before:**
```properties
FRONTEND_URL=${FRONTEND_URL:https://hack-2-hired.vercel.app}
BACKEND_URL=${BACKEND_URL:https://placement-portal-backend-production.up.railway.app}
```

**After:**
```properties
# FRONTEND CONFIGURATION
# Use environment variable if set, otherwise use default Render URL
frontend.url=${FRONTEND_URL:https://hack-2-hired.onrender.com}

# BACKEND CONFIGURATION
# Use environment variable if set, otherwise use default Render URL
backend.url=${BACKEND_URL:https://placement-portal-backend-nwaj.onrender.com}
```

### 2. **Updated Java Files**
Updated all `@Value` annotations to use the new property names:

#### **SecurityConfig.java**
```java
// Before:
@Value("${FRONTEND_URL}")
private String frontendUrl;

// After:
@Value("${frontend.url}")
private String frontendUrl;
```

Also added new Render URL to CORS allowed origins:
```java
configuration.setAllowedOrigins(List.of(
    frontendUrl,
    "https://hack-2-hired.onrender.com",  // NEW
    "https://hack-2-hired.vercel.app",
    "https://hack-2-hired.vercel.app/"));
```

#### **EmailService.java**
```java
// Before:
@Value("${FRONTEND_URL}")
private String frontendUrl;

// After:
@Value("${frontend.url}")
private String frontendUrl;
```

#### **WebConfig.java**
```java
// Before:
@Value("$ {FRONTEND_URL}")  // Also had a space issue
private String frontendUrl;

// After:
@Value("${frontend.url}")
private String frontendUrl;
```

#### **GalleryController.java**
```java
// Before:
@Value("${BACKEND_URL}")
private String backendUrl;

// After:
@Value("${backend.url}")
private String backendUrl;
```

### 3. **Updated Frontend Config**
Updated `fully-frontend-react/src/config.js`:

**Before:**
```javascript
const API_BASE_URL = "https://placement-portal-backend-production.up.railway.app/api";
```

**After:**
```javascript
const API_BASE_URL = "https://placement-portal-backend-nwaj.onrender.com/api";
```

## 📋 Files Modified

### Backend:
1. `src/main/resources/application.properties`
2. `src/main/java/com/abhi/authProject/config/SecurityConfig.java`
3. `src/main/java/com/abhi/authProject/config/WebConfig.java`
4. `src/main/java/com/abhi/authProject/service/EmailService.java`
5. `src/main/java/com/abhi/authProject/controller/GalleryController.java`

### Frontend:
1. `src/config.js`

## 🚀 Deployment URLs

### Production URLs (Render):
- **Frontend**: https://hack-2-hired.onrender.com
- **Backend**: https://placement-portal-backend-nwaj.onrender.com

### Legacy URLs (Still supported):
- **Frontend**: https://hack-2-hired.vercel.app
- **Backend**: https://placement-portal-backend-production.up.railway.app

## 🔄 Environment Variables (Optional)

If you want to override the default URLs in your Render deployment, set these environment variables:

```bash
FRONTEND_URL=https://your-custom-frontend-url.com
BACKEND_URL=https://your-custom-backend-url.com
```

The application will use these if set, otherwise it will fall back to the Render URLs.

## ✅ What This Fixes

1. ✅ Eliminates circular dependency error
2. ✅ Application will start successfully
3. ✅ CORS configured for both Render and Vercel deployments
4. ✅ Frontend points to correct backend API
5. ✅ Email links will use correct frontend URL
6. ✅ Gallery images will use correct backend URL

## 🧪 Testing

After deploying, test:
1. Backend health check: `https://placement-portal-backend-nwaj.onrender.com/api/health`
2. Frontend loads correctly
3. Login/Register functionality
4. API calls work from frontend to backend
5. CORS is working (no CORS errors in browser console)

## 📝 Notes

- The property naming convention changed from `UPPERCASE_WITH_UNDERSCORES` to `lowercase.with.dots` which is the Spring Boot standard
- Both Vercel and Render URLs are supported in CORS for backward compatibility
- Environment variables still use the original `FRONTEND_URL` and `BACKEND_URL` names for consistency with your deployment platform

---

**Issue Resolved**: ✅ Circular dependency eliminated, application ready to deploy!
