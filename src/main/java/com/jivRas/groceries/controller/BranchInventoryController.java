package com.jivRas.groceries.controller;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jivRas.groceries.dto.BranchInventoryRequest;
import com.jivRas.groceries.dto.BranchInventoryResponse;
import com.jivRas.groceries.entity.EmployeeUser;
import com.jivRas.groceries.repository.EmployeeUserRepository;
import com.jivRas.groceries.service.BranchInventoryService;
import com.jivRas.groceries.service.DynamicAuthorizationService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class BranchInventoryController {

    private final BranchInventoryService branchInventoryService;
    private final EmployeeUserRepository employeeUserRepository;
    private final DynamicAuthorizationService dynamicAuthorizationService;

    /**
     * GET /api/inventory/my-branch
     *
     * For EMPLOYEE and BRANCH_MANAGER.
     * Reads the branchId from the employee's own profile (not from JWT claim).
     * This ensures the employee can ONLY see their own branch.
     */
    @GetMapping("/my-branch")
    public ResponseEntity<?> getMyBranchInventory(
            Principal principal,
            HttpServletRequest httpRequest,
            Authentication authentication) {

        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        String role = resolveRole(authentication);
        if (!dynamicAuthorizationService.isAllowed(role, httpRequest.getRequestURI(), httpRequest.getMethod())) {
            return ResponseEntity.status(403).body("Access denied");
        }

        Optional<EmployeeUser> employeeOpt = employeeUserRepository.findByUsername(principal.getName());
        if (employeeOpt.isEmpty()) {
            return ResponseEntity.status(404).body("Employee profile not found");
        }

        Long branchId = employeeOpt.get().getBranchId();
        if (branchId == null) {
            return ResponseEntity.badRequest().body("No branch assigned to this account");
        }

        List<BranchInventoryResponse> inventory = branchInventoryService.getInventoryByBranch(branchId);
        return ResponseEntity.ok(inventory);
    }

    /**
     * GET /api/inventory/branch/{branchId}
     *
     * For ADMIN only — can view any branch's inventory.
     */
    @GetMapping("/branch/{branchId}")
    public ResponseEntity<?> getBranchInventory(
            @PathVariable Long branchId,
            HttpServletRequest httpRequest,
            Authentication authentication) {

        String role = resolveRole(authentication);
        if (!dynamicAuthorizationService.isAllowed(role, httpRequest.getRequestURI(), httpRequest.getMethod())) {
            return ResponseEntity.status(403).body("Access denied");
        }

        List<BranchInventoryResponse> inventory = branchInventoryService.getInventoryByBranch(branchId);
        return ResponseEntity.ok(inventory);
    }

    /**
     * POST /api/inventory/stock
     *
     * Add or update stock for a branch+product.
     * ADMIN can update any branch. EMPLOYEE/BRANCH_MANAGER can only update their own branch.
     */
    @PostMapping("/stock")
    public ResponseEntity<?> addOrUpdateStock(
            @RequestBody BranchInventoryRequest request,
            Principal principal,
            HttpServletRequest httpRequest,
            Authentication authentication) {

        String role = resolveRole(authentication);
        if (!dynamicAuthorizationService.isAllowed(role, httpRequest.getRequestURI(), httpRequest.getMethod())) {
            return ResponseEntity.status(403).body("Access denied");
        }

        // For non-admin users, enforce that they can only update their own branch
        Optional<EmployeeUser> employeeOpt = employeeUserRepository.findByUsername(principal.getName());
        if (employeeOpt.isPresent()) {
            EmployeeUser employee = employeeOpt.get();
            boolean isAdmin = "ADMIN".equals(employee.getRole());

            if (!isAdmin && employee.getBranchId() != null
                    && !employee.getBranchId().equals(request.getBranchId())) {
                return ResponseEntity.status(403).body("You can only update your own branch's inventory");
            }
        }

        BranchInventoryResponse response = branchInventoryService.addOrUpdateStock(request);
        return ResponseEntity.ok(response);
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
