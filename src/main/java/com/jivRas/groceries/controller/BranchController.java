package com.jivRas.groceries.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jivRas.groceries.dto.BranchRequest;
import com.jivRas.groceries.dto.BranchResponse;
import com.jivRas.groceries.service.BranchService;

@RestController
@RequestMapping("/api/branches")
public class BranchController {

    private final BranchService branchService;

    public BranchController(BranchService branchService) {
        this.branchService = branchService;
    }

    /**
     * POST /api/branches
     * ADMIN only — create a new branch.
     */

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')") // This correctly looks for "ROLE_ADMIN"
    public ResponseEntity<BranchResponse> createBranch(@RequestBody BranchRequest request) {
        return ResponseEntity.ok(branchService.createBranch(request));
    }

    /**
     * GET /api/branches/active
     * Returns all active branches. Used by dashboard branch dropdown.
     */
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER', 'EMPLOYEE')")
    public ResponseEntity<List<BranchResponse>> getAllActiveBranches() {
        return ResponseEntity.ok(branchService.getAllActiveBranches());
    }
}
