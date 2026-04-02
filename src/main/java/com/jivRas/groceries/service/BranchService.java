package com.jivRas.groceries.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.jivRas.groceries.dto.BranchRequest;
import com.jivRas.groceries.dto.BranchResponse;
import com.jivRas.groceries.entity.Branch;
import com.jivRas.groceries.exception.ResourceNotFoundException;
import com.jivRas.groceries.repository.BranchRepository;

@Service
public class BranchService {

    // Injecting BranchRepository via constructor (no @Autowired — constructor injection is best practice)
    private final BranchRepository branchRepository;

    public BranchService(BranchRepository branchRepository) {
        this.branchRepository = branchRepository;
    }

    /**
     * CREATE a new branch.
     * Only ADMIN should call this endpoint (enforced at controller level).
     *
     * @param request — branch details from frontend
     * @return BranchResponse — saved branch details
     */
    public BranchResponse createBranch(BranchRequest request) {

        // Build the Branch entity from incoming request
        Branch branch = new Branch();
        branch.setName(request.getName());
        branch.setAddress(request.getAddress());
        branch.setPincode(request.getPincode());
        branch.setCity(request.getCity());
        branch.setManagerUsername(request.getManagerUsername());
        branch.setActive(true);                         // New branch is active by default
        branch.setCreatedAt(LocalDateTime.now());       // Set creation timestamp

        // Save to DB
        Branch saved = branchRepository.save(branch);

        // Convert entity to response DTO and return
        return mapToResponse(saved);
    }

    /**
     * GET all active branches.
     * ADMIN sees all. Used on Super Admin dashboard.
     *
     * @return list of all active BranchResponse objects
     */
    public List<BranchResponse> getAllActiveBranches() {

        // Fetch all branches where active = true
        List<Branch> branches = branchRepository.findByActiveTrue();

        // Convert each Branch entity to BranchResponse DTO
        List<BranchResponse> responseList = new ArrayList<>();
        for (Branch branch : branches) {
            responseList.add(mapToResponse(branch));
        }

        return responseList;
    }

    /**
     * GET a single branch by its ID.
     * Used when loading a specific branch dashboard.
     *
     * @param branchId — ID of the branch to fetch
     * @return BranchResponse
     */
    public BranchResponse getBranchById(Long branchId) {

        // Find branch by ID — throws exception if not found
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + branchId));

        return mapToResponse(branch);
    }

    /**
     * UPDATE branch details.
     * ADMIN only. Can update name, address, pincode, manager etc.
     *
     * @param branchId — which branch to update
     * @param request  — new values to apply
     * @return updated BranchResponse
     */
    public BranchResponse updateBranch(Long branchId, BranchRequest request) {

        // Find existing branch
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + branchId));

        // Update only non-null fields
        if (request.getName() != null) {
            branch.setName(request.getName());
        }
        if (request.getAddress() != null) {
            branch.setAddress(request.getAddress());
        }
        if (request.getPincode() != null) {
            branch.setPincode(request.getPincode());
        }
        if (request.getCity() != null) {
            branch.setCity(request.getCity());
        }
        if (request.getManagerUsername() != null) {
            branch.setManagerUsername(request.getManagerUsername());
        }

        // Save updated branch
        Branch updated = branchRepository.save(branch);
        return mapToResponse(updated);
    }

    /**
     * SOFT DELETE a branch — sets active = false.
     * We never hard-delete branches to preserve historical data (orders, inventory).
     *
     * @param branchId — which branch to deactivate
     */
    public void deactivateBranch(Long branchId) {

        // Find the branch
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + branchId));

        // Set active to false — soft delete
        branch.setActive(false);
        branchRepository.save(branch);
    }

    /**
     * Helper method — converts Branch entity to BranchResponse DTO.
     * Called internally by all methods above.
     */
    private BranchResponse mapToResponse(Branch branch) {
        BranchResponse response = new BranchResponse();
        response.setId(branch.getId());
        response.setName(branch.getName());
        response.setAddress(branch.getAddress());
        response.setPincode(branch.getPincode());
        response.setCity(branch.getCity());
        response.setManagerUsername(branch.getManagerUsername());
        response.setActive(branch.isActive());
        return response;
    }
}