# CORS Issue Fix Summary

## Problem
CORS (Cross-Origin Resource Sharing) errors were occurring during login, preventing the frontend from communicating with the backend API.

## Root Causes Identified & Fixed

### 1. **Frontend URL Mismatch in Backend Configuration** ✅ FIXED
   - **Issue**: `application.properties` had `frontend.url=http://localhost:4200`
   - **Actual Frontend**: Running on `http://localhost:4400`
   - **Impact**: CorsConfig was allowing wrong origin
   - **Fix**: Updated `frontend.url=http://localhost:4400`
   - **File**: `src/main/resources/application.properties`

### 2. **Hardcoded CORS Origins in Controllers** ✅ FIXED
   - **Issue**: UserController and ProductController had hardcoded `@CrossOrigin(origins = "http://localhost:4400")`
   - **Problem**: Not flexible, difficult to maintain, duplicates CorsConfig
   - **Fix**: Removed `@CrossOrigin` annotations and rely on centralized CorsConfig
   - **Files Modified**:
     - `src/main/java/com/jivRas/groceries/controller/UserController.java`
     - `src/main/java/com/jivRas/groceries/controller/ProductController.java`

### 3. **CorsConfig Setup** ✅ VERIFIED
   - The centralized `CorsConfig.java` reads `frontend.url` from properties
   - Applies CORS to all endpoints (`/**`)
   - Allows all HTTP methods (GET, POST, PUT, DELETE, OPTIONS)
   - Now with correct frontend URL: `http://localhost:4400`

### 4. **SecurityConfig CORS Support** ✅ VERIFIED
   - `.cors(Customizer.withDefaults())` properly enables CORS
   - OPTIONS requests are allowed for preflight checks
   - `/api/users/**` endpoint is permitted (includes login)

## Architecture Overview

```
Frontend (http://localhost:4400)
    ↓ CORS Request
Backend (http://localhost:8080)
    ├── CorsConfig (reads frontend.url from properties)
    ├── SecurityConfig (enables CORS)
    └── Controllers (no longer need hardcoded @CrossOrigin)
```

## Updated Configuration

### application.properties
```properties
frontend.url=http://localhost:4400
```

### CorsConfig.java
```java
@Value("${frontend.url}")
private String frontendUrl;

registry.addMapping("/**")
    .allowedOrigins(frontendUrl)
    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
    .allowedHeaders("*")
    .allowCredentials(true);
```

## How CORS Preflight Works

When frontend makes a login request:
1. Browser sends OPTIONS preflight request
2. Backend CorsConfig responds with allowed origins/methods/headers
3. Browser allows actual POST request to proceed
4. Login request succeeds

## Testing the Fix

### 1. Stop and Rebuild Backend
```bash
cd E:\JivRas\groceries(2)\groceries
mvn clean install
mvn spring-boot:run
```

### 2. Start Frontend
```bash
cd grocery-frontend
npm start
```

### 3. Test Login
1. Open browser DevTools (F12)
2. Go to Network tab
3. Click login
4. Look for OPTIONS request (preflight) - should have Status: 200 or 204
5. Look for POST request (actual login) - should have Status: 200
6. In Response Headers, should see: `access-control-allow-origin: http://localhost:4400`

### 4. Verify Success
- Login should succeed
- Products should load
- Add Product button should show (if owner)
- No CORS errors in Console tab

## Files Changed Summary

| File | Change | Reason |
|------|--------|--------|
| `application.properties` | Updated `frontend.url` from 4200 → 4400 | Match actual frontend port |
| `UserController.java` | Removed `@CrossOrigin` annotation | Use centralized CorsConfig |
| `ProductController.java` | Removed `@CrossOrigin` annotation | Use centralized CorsConfig |

## Troubleshooting CORS Issues

If CORS errors persist:

1. **Check browser console for error message**
   - Look for "Access to XMLHttpRequest blocked by CORS policy"

2. **Verify backend is running on port 8080**
   - Check in application.properties: `server.port=8080`

3. **Verify frontend URL in properties**
   - Run: `grep frontend.url src/main/resources/application.properties`
   - Should show: `frontend.url=http://localhost:4400`

4. **Check Network tab in DevTools**
   - OPTIONS request status should be 200/204
   - Response Headers should have `access-control-allow-origin`

5. **Restart both services**
   - Backend changes require full restart
   - Frontend needs rebuild: `npm run build` or `ng build`

## Security Notes

- CORS is properly configured with specific origin
- Credentials are allowed for same-site requests
- All HTTP methods are allowed only for specified origin
- JWT tokens are included in Authorization header (via interceptor)
