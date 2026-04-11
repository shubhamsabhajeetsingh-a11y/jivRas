package com.jivRas.groceries.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller method with its logical module and action.
 *
 * <p>The {@link com.jivRas.groceries.config.ModuleActionScanner} reads these
 * annotations at startup and builds an in-memory registry mapping
 * (httpMethod, uriPattern) → (module, action).
 *
 * <p>The {@link com.jivRas.groceries.service.DynamicAuthorizationService} uses that
 * registry to resolve an incoming request to a module+action pair, then queries
 * {@code role_permissions} to decide whether the caller is allowed to proceed.
 *
 * <p>Do NOT place this annotation on auth endpoints under {@code /api/auth/**} —
 * those are declared {@code permitAll()} in SecurityConfig and bypass RBAC entirely.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModuleAction {

    /** Logical module name, e.g. "INVENTORY", "ORDERS", "PRODUCTS". */
    String module();

    /** Logical action name, e.g. "VIEW", "CREATE", "EDIT", "DELETE". */
    String action();
}
