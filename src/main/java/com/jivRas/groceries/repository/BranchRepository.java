package com.jivRas.groceries.repository;
 
import java.util.List;
import java.util.Optional;
 
import org.springframework.data.jpa.repository.JpaRepository;
import com.jivRas.groceries.entity.Branch;
 
public interface BranchRepository extends JpaRepository<Branch, Long> {
 
    // Get all branches that are currently active (not soft-deleted)
    List<Branch> findByActiveTrue();
 
    // Find branch by pincode — used for order routing logic
    // When customer places order, we find their nearest branch by pincode
    Optional<Branch> findByPincodeAndActiveTrue(String pincode);
 
    // Find branch assigned to a specific manager
    Optional<Branch> findByManagerUsername(String managerUsername);
}