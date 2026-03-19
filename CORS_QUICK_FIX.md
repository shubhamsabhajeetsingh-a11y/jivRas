# Quick Fix Reference - CORS Login Issue

## What Was Wrong
❌ `frontend.url=http://localhost:4200` (in application.properties)  
✅ Frontend actually runs on `http://localhost:4400`

## What Was Fixed
1. ✅ Updated `application.properties`: Changed `frontend.url` to `http://localhost:4400`
2. ✅ Removed hardcoded `@CrossOrigin` from UserController and ProductController
3. ✅ All CORS now centrally managed by CorsConfig using the property value

## How to Run

### Terminal 1 - Start Backend
```bash
cd E:\JivRas\groceries(2)\groceries
mvn spring-boot:run
```

### Terminal 2 - Start Frontend  
```bash
cd E:\JivRas\groceries(2)\groceries\grocery-frontend
npm start
```

## Expected Result
✅ No CORS errors in browser console  
✅ Login works without errors  
✅ Products load successfully  
✅ Can navigate to add-product page (if owner)  
✅ Can add new products  

## Verify in Browser DevTools (F12)
- **Console**: No CORS errors
- **Network**: 
  - OPTIONS request for /api/users/login → 200/204 ✅
  - POST request for /api/users/login → 200 ✅
  - See `access-control-allow-origin: http://localhost:4400` in response headers

## If Still Getting CORS Errors
1. ❌ Stop backend completely
2. ❌ Run: `mvn clean install` (to ensure fresh build)
3. ❌ Run: `mvn spring-boot:run`
4. ❌ Hard refresh frontend (Ctrl+Shift+R)
