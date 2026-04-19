package com.jivRas.groceries.controller;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jivRas.groceries.annotation.ModuleAction;
import com.jivRas.groceries.dto.BranchInventoryRequest;
import com.jivRas.groceries.dto.BranchInventoryResponse;
import com.jivRas.groceries.dto.BulkStockUpdateRequest;
import com.jivRas.groceries.dto.StockTransferRequest;
import com.jivRas.groceries.entity.EmployeeUser;
import com.jivRas.groceries.repository.EmployeeUserRepository;
import com.jivRas.groceries.service.BranchInventoryService;
import com.jivRas.groceries.service.DynamicAuthorizationService;
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
    @ModuleAction(module = "INVENTORY", action = "VIEW")
    @GetMapping("/my-branch")
    public ResponseEntity<?> getMyBranchInventory(Principal principal) {

        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized");
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
    @ModuleAction(module = "INVENTORY", action = "VIEW")
    @GetMapping("/branch/{branchId}")
    public ResponseEntity<?> getBranchInventory(@PathVariable Long branchId) {
        List<BranchInventoryResponse> inventory = branchInventoryService.getInventoryByBranch(branchId);
        return ResponseEntity.ok(inventory);
    }

    /**
     * POST /api/inventory/stock
     *
     * Add or update stock for a branch+product.
     * ADMIN can update any branch. EMPLOYEE/BRANCH_MANAGER can only update their own branch.
     */
    @ModuleAction(module = "INVENTORY", action = "CREATE")
    @PostMapping("/stock")
    public ResponseEntity<?> addOrUpdateStock(
            @RequestBody BranchInventoryRequest request,
            Principal principal) {

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

    @ModuleAction(module = "INVENTORY", action = "EDIT")
    @PutMapping("/bulk-update")
    public ResponseEntity<?> bulkUpdate(@RequestBody BulkStockUpdateRequest request) {
        dynamicAuthorizationService.evictPermissionCache();
        List<BranchInventoryResponse> response = branchInventoryService.bulkUpdateStock(request);
        return ResponseEntity.ok(response);
    }

    @ModuleAction(module = "INVENTORY", action = "CREATE")
    @PostMapping("/transfer")
    public ResponseEntity<?> transfer(@RequestBody StockTransferRequest request) {
        dynamicAuthorizationService.evictPermissionCache();
        java.util.Map<String, Object> response = branchInventoryService.transferStock(request);
        return ResponseEntity.ok(response);
    }

}
