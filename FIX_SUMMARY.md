The user was unable to view items after logging in. The root cause was a CORS misconfiguration on the backend.

The following changes were made:
- Removed the `@CrossOrigin(origins = "http://localhost:4400")` annotation from `ProductController.java` and `UserController.java`.
- This allows the global CORS configuration in `CorsConfig.java` to take effect, which correctly allows requests from the frontend running on `http://localhost:4200`.