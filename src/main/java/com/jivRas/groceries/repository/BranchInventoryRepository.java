package com.jivRas.groceries.repository;
 
import java.util.List;
import java.util.Optional;
 
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
 
import com.jivRas.groceries.entity.BranchInventory;
 
public interface BranchInventoryRepository extends JpaRepository<BranchInventory, Long> {
 
    // Get all inventory entries for a specific branch
    // Used by Employee/BranchManager dashboard to show their branch stock
    List<BranchInventory> findByBranch_Id(Long branchId);

    // Get stock entry for a specific product in a specific branch
    // Used when placing an order to check and decrement stock
    Optional<BranchInventory> findByBranch_IdAndProduct_Id(Long branchId, Long productId);

    // Get all inventory entries for a specific product across ALL branches
    // Used by Super Admin to see consolidated stock view
    List<BranchInventory> findByProduct_Id(Long productId);

 
    /**
     * Get all LOW STOCK items for a specific branch.
     * Low stock = availableStockKg is less than or equal to lowStockThreshold
     *
     * Used to show warning alerts on the dashboard.
     */
    @Query("SELECT bi FROM BranchInventory bi WHERE bi.branch.id = :branchId AND bi.availableStockKg <= bi.lowStockThreshold")
    List<BranchInventory> findLowStockByBranchId(@Param("branchId") Long branchId);
 
    /**
     * Get ALL low stock items across ALL branches.
     * Used by Super Admin for a system-wide stock alert view.
     */
    @Query("SELECT bi FROM BranchInventory bi WHERE bi.availableStockKg <= bi.lowStockThreshold")
    List<BranchInventory> findAllLowStock();
}