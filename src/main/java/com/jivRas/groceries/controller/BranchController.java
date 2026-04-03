package com.jivRas.groceries.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jivRas.groceries.dto.BranchRequest;
import com.jivRas.groceries.dto.BranchResponse;
import com.jivRas.groceries.service.BranchService;
import com.jivRas.groceries.service.DynamicAuthorizationService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;
    private final DynamicAuthorizationService dynamicAuthorizationService;

    /**
     * POST /api/branches
     * ADMIN only — create a new branch.
     */
    @PostMapping
    public ResponseEntity<?> createBranch(
            @RequestBody BranchRequest request,
            HttpServletRequest httpRequest,
            Authentication authentication) {

        String role = resolveRole(authentication);
        if (!dynamicAuthorizationService.isAllowed(role, httpRequest.getRequestURI(), httpRequest.getMethod())) {
            return ResponseEntity.status(403).body("Access denied");
        }
        return ResponseEntity.ok(branchService.createBranch(request));
    }

    /**
     * GET /api/branches/active
     * Returns all active branches. Used by dashboard branch dropdown.
     */
    @GetMapping("/active")
    public ResponseEntity<?> getAllActiveBranches(
            HttpServletRequest httpRequest,
            Authentication authentication) {

        String role = resolveRole(authentication);
        if (!dynamicAuthorizationService.isAllowed(role, httpRequest.getRequestURI(), httpRequest.getMethod())) {
            return ResponseEntity.status(403).body("Access denied");
        }
        List<BranchResponse> branches = branchService.getAllActiveBranches();
        return ResponseEntity.ok(branches);
    }

    // ── Utility ─────────────────────────────────────────────────────────────

    private String resolveRole(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return "";
        return authentication.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .findFirst()
                .orElse("");
    }
}
