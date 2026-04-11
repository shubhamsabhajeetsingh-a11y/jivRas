package com.jivRas.groceries.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.jivRas.groceries.annotation.ModuleAction;

/**
 * Scans all {@code @RestController} handler methods annotated with
 * {@link ModuleAction} after the Spring context is fully started, builds an
 * in-memory registry of {@code (httpMethod, uriPattern) → (module, action)}
 * entries, and exposes a lookup helper used by
 * {@link com.jivRas.groceries.service.DynamicAuthorizationService}.
 *
 * <p>Startup order: Spring fires {@link ContextRefreshedEvent} <em>after</em>
 * all beans are wired and {@link RequestMappingHandlerMapping} is fully
 * populated, so this scanner is guaranteed to see every registered route.
 */
@Component
public class ModuleActionScanner implements ApplicationListener<ContextRefreshedEvent> {

    private final RequestMappingHandlerMapping requestMappingHandlerMapping;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    /** Populated once on the first context-refresh event. */
    private final List<ModuleActionEntry> registry = new ArrayList<>();

    /** Guard so we only scan once even if the context is refreshed multiple times
     *  (e.g. when running tests). */
    private volatile boolean scanned = false;

    public ModuleActionScanner(RequestMappingHandlerMapping requestMappingHandlerMapping) {
        this.requestMappingHandlerMapping = requestMappingHandlerMapping;
    }

    // ── ApplicationListener ──────────────────────────────────────────────────

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (scanned) return;
        scanned = true;

        Map<RequestMappingInfo, HandlerMethod> handlerMethods =
                requestMappingHandlerMapping.getHandlerMethods();

        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods.entrySet()) {
            HandlerMethod handlerMethod = entry.getValue();

            // Only process methods that carry @ModuleAction
            ModuleAction annotation = handlerMethod.getMethodAnnotation(ModuleAction.class);
            if (annotation == null) continue;

            String module = annotation.module();
            String action = annotation.action();

            RequestMappingInfo mappingInfo = entry.getKey();

            // Extract URI patterns
            Set<String> patterns = extractPatterns(mappingInfo);

            // Extract HTTP methods (empty set means all methods)
            Set<org.springframework.web.bind.annotation.RequestMethod> httpMethods =
                    mappingInfo.getMethodsCondition().getMethods();

            if (httpMethods.isEmpty()) {
                // No HTTP method constraint — register with wildcard "*"
                for (String pattern : patterns) {
                    registry.add(new ModuleActionEntry("*", pattern, module, action));
                }
            } else {
                for (org.springframework.web.bind.annotation.RequestMethod method : httpMethods) {
                    for (String pattern : patterns) {
                        registry.add(new ModuleActionEntry(method.name(), pattern, module, action));
                    }
                }
            }
        }

        System.out.printf("[ModuleActionScanner] Registered %d module-action mappings%n", registry.size());
    }

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Resolves an incoming request to a {@code [module, action]} pair.
     *
     * @param httpMethod uppercase HTTP verb, e.g. {@code "GET"}
     * @param uri        the actual request URI, e.g. {@code "/api/orders/5"}
     * @return {@code String[2]} where index 0 = module, index 1 = action;
     *         {@code null} if no matching entry is found
     */
    public String[] resolveModuleAction(String httpMethod, String uri) {
        if (httpMethod == null || uri == null) return null;

        // Strip query string
        String path = uri.contains("?") ? uri.substring(0, uri.indexOf('?')) : uri;

        for (ModuleActionEntry entry : registry) {
            boolean methodMatch = "*".equals(entry.httpMethod)
                    || entry.httpMethod.equalsIgnoreCase(httpMethod);
            if (!methodMatch) continue;

            if (antPathMatcher.match(entry.uriPattern, path)) {
                return new String[]{entry.module, entry.action};
            }
        }
        return null;
    }

    /**
     * Returns a read-only view of the complete registry.
     * Useful for the {@code GET /api/role-permissions/modules} admin endpoint.
     */
    public List<ModuleActionEntry> getRegistry() {
        return Collections.unmodifiableList(registry);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Extracts URI pattern strings from a {@link RequestMappingInfo}.
     * Works with both the pattern-based and path-based condition APIs.
     */
    private Set<String> extractPatterns(RequestMappingInfo mappingInfo) {
        // Spring 5.3+ prefers getPatternsCondition(); fall back to
        // getDirectPaths() when patterns are not available.
        if (mappingInfo.getPatternsCondition() != null) {
            return mappingInfo.getPatternsCondition().getPatterns();
        }
        // PathPatternsCondition (Spring 6 / Spring Boot 3)
        if (mappingInfo.getPathPatternsCondition() != null) {
            Set<String> result = new java.util.LinkedHashSet<>();
            mappingInfo.getPathPatternsCondition()
                    .getPatterns()
                    .forEach(p -> result.add(p.getPatternString()));
            return result;
        }
        return Collections.emptySet();
    }

    // ── Inner data class ─────────────────────────────────────────────────────

    /**
     * Immutable record representing a single (method, pattern) → (module, action)
     * mapping discovered during startup scanning.
     */
    public static class ModuleActionEntry {

        public final String httpMethod;
        public final String uriPattern;
        public final String module;
        public final String action;

        public ModuleActionEntry(String httpMethod, String uriPattern,
                                  String module, String action) {
            this.httpMethod = httpMethod;
            this.uriPattern = uriPattern;
            this.module = module;
            this.action = action;
        }

        @Override
        public String toString() {
            return httpMethod + " " + uriPattern + " → " + module + ":" + action;
        }
    }
}
