package com.jivRas.groceries.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.jivRas.groceries.annotation.ModuleAction;
import com.jivRas.groceries.service.DynamicAuthorizationService;

/**
 * AOP advice that enforces RBAC on every controller method annotated
 * with {@link ModuleAction}.
 *
 * <p>Replaces the boilerplate {@code dynamicAuthorizationService.isAllowed(...)}
 * blocks that were previously repeated inside each controller method.
 */
@Aspect
@Component
public class ModuleActionAspect {

    private final DynamicAuthorizationService dynamicAuthorizationService;

    public ModuleActionAspect(DynamicAuthorizationService dynamicAuthorizationService) {
        this.dynamicAuthorizationService = dynamicAuthorizationService;
    }

    @Around("@annotation(moduleAction)")
    public Object enforce(ProceedingJoinPoint pjp, ModuleAction moduleAction) throws Throwable {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        String role = (auth == null || auth.getAuthorities() == null || auth.getAuthorities().isEmpty())
                ? "GUEST"
                : auth.getAuthorities().iterator().next().getAuthority();

        boolean allowed = dynamicAuthorizationService.isAllowedByModuleAction(
                role, moduleAction.module(), moduleAction.action());

        if (!allowed) {
            throw new AccessDeniedException(
                    "Access denied: role [" + role + "] is not permitted to perform "
                    + moduleAction.module() + ":" + moduleAction.action());
        }

        return pjp.proceed();
    }
}
