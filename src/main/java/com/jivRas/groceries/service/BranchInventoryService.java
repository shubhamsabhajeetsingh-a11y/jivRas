package com.jivRas.groceries.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jivRas.groceries.dto.BranchInventoryRequest;
import com.jivRas.groceries.dto.BranchInventoryResponse;
import com.jivRas.groceries.dto.BulkStockUpdateRequest;
import com.jivRas.groceries.dto.StockTransferRequest;
import com.jivRas.groceries.entity.Branch;
import com.jivRas.groceries.entity.BranchInventory;
import com.jivRas.groceries.entity.Product;
import com.jivRas.groceries.exception.ResourceNotFoundException;
import com.jivRas.groceries.repository.BranchInventoryRepository;
import com.jivRas.groceries.repository.BranchRepository;
import com.jivRas.groceries.repository.ProductRepository;

@Service
public class BranchInventoryService {

    private final BranchInventoryRepository branchInventoryRepository;
    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;

    public BranchInventoryService(
            BranchInventoryRepository branchInventoryRepository,
            BranchRepository branchRepository,
            ProductRepository productRepository) {
        this.branchInventoryRepository = branchInventoryRepository;
        this.branchRepository = branchRepository;
        this.productRepository = productRepository;
    }

    /**
     * ADD or UPDATE stock for a product in a specific branch.
     *
     * Logic:
     *   - If a BranchInventory entry already exists for this branch+product → update stock
     *   - If no entry exists yet → create a new one
     *
     * Called when:
     *   - Admin adds a product to a new branch for the first time
     *   - Employee or Branch Manager restocks a product
     *
     * @param request — contains branchId, productId, stock quantity, threshold
     * @return saved BranchInventoryResponse
     */
    public BranchInventoryResponse addOrUpdateStock(BranchInventoryRequest request) {

        // Step 1: Validate that branch exists
        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found: " + request.getBranchId()));

        // Step 2: Validate that product exists
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + request.getProductId()));

        // Step 3: Check if a BranchInventory entry already exists for this branch + product
        BranchInventory inventory = branchInventoryRepository
                .findByBranch_IdAndProduct_Id(request.getBranchId(), request.getProductId())
                .orElse(new BranchInventory()); // If not found, create new empty object

        // Step 4: Set all fields — works for both create and update
        inventory.setBranch(branch);
        inventory.setProduct(product);
        inventory.setAvailableStockKg(request.getAvailableStockKg());
        inventory.setLastUpdated(LocalDateTime.now());

        // Step 5: Set threshold — use provided value or default to 5.0 if not given
        if (request.getLowStockThreshold() != null) {
            inventory.setLowStockThreshold(request.getLowStockThreshold());
        } else if (inventory.getLowStockThreshold() == null) {
            inventory.setLowStockThreshold(5.0); // Default threshold
        }

        // Step 6: Save and return
        BranchInventory saved = branchInventoryRepository.save(inventory);
        return mapToResponse(saved);
    }

    /**
     * GET all inventory for a specific branch.
     * Used by Employee/BranchManager dashboard.
     *
     * The branchId comes from the JWT token (not from request param)
     * so an employee can ONLY see their own branch.
     *
     * @param branchId — extracted from JWT in controller
     * @return list of stock entries for that branch
     */
    public List<BranchInventoryResponse> getInventoryByBranch(Long branchId) {

        // Fetch all stock entries for this branch
        List<BranchInventory> inventoryList = branchInventoryRepository.findByBranch_Id(branchId);

        // Convert each entity to DTO
        List<BranchInventoryResponse> responseList = new ArrayList<>();
        for (BranchInventory inv : inventoryList) {
            responseList.add(mapToResponse(inv));
        }

        return responseList;
    }

    /**
     * GET inventory for a specific product across ALL branches.
     * Used by Super Admin for consolidated product stock view.
     *
     * Example: "How much Basmati Rice is left in each branch?"
     *
     * @param productId — which product to check
     * @return stock entries for this product in every branch
     */
    public List<BranchInventoryResponse> getStockAcrossAllBranches(Long productId) {

        List<BranchInventory> inventoryList = branchInventoryRepository.findByProduct_Id(productId);

        List<BranchInventoryResponse> responseList = new ArrayList<>();
        for (BranchInventory inv : inventoryList) {
            responseList.add(mapToResponse(inv));
        }

        return responseList;
    }

    /**
     * GET all low stock alerts for a specific branch.
     * Items where availableStockKg <= lowStockThreshold are returned.
     *
     * Dashboard shows these in red/yellow warning state.
     *
     * @param branchId — which branch to check
     * @return list of low stock items
     */
    public List<BranchInventoryResponse> getLowStockAlerts(Long branchId) {

        List<BranchInventory> lowStockList = branchInventoryRepository.findLowStockByBranchId(branchId);

        List<BranchInventoryResponse> responseList = new ArrayList<>();
        for (BranchInventory inv : lowStockList) {
            responseList.add(mapToResponse(inv));
        }

        return responseList;
    }

    /**
     * GET system-wide low stock alerts across ALL branches.
     * Only Super Admin sees this.
     *
     * @return all low stock entries from every branch
     */
    public List<BranchInventoryResponse> getAllLowStockAlerts() {

        List<BranchInventory> lowStockList = branchInventoryRepository.findAllLowStock();

        List<BranchInventoryResponse> responseList = new ArrayList<>();
        for (BranchInventory inv : lowStockList) {
            responseList.add(mapToResponse(inv));
        }

        return responseList;
    }

    /**
     * TRANSFER stock from one branch to another.
     *
     * Example: Vasai has 50kg Toor Dal. Nalasopara is out.
     * Admin/Manager transfers 20kg from Vasai → Nalasopara.
     *
     * This is an ATOMIC operation — both branches update together.
     * If any step fails, the entire transaction rolls back.
     *
     * @param request — fromBranchId, toBranchId, productId, quantity
     */
    @Transactional  // Ensures both deduct + add happen atomically
    public Map<String, Object> transferStock(StockTransferRequest request) {

        // Step 1: Get stock entry at source branch (from where stock is going)
        BranchInventory fromInventory = branchInventoryRepository
                .findByBranch_IdAndProduct_Id(request.getFromBranchId(), request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found in source branch: " + request.getFromBranchId()));

        // Step 2: Check if source branch has enough stock
        if (fromInventory.getAvailableStockKg() < request.getQuantity()) {
            throw new RuntimeException("Insufficient stock for transfer");
        }

        // Step 3: Get stock entry at destination branch (where stock is going)
        BranchInventory toInventory = branchInventoryRepository
                .findByBranch_IdAndProduct_Id(request.getToBranchId(), request.getProductId())
                .orElseGet(() -> {
                    BranchInventory newInv = new BranchInventory();
                    Branch destBranch = branchRepository.findById(request.getToBranchId())
                            .orElseThrow(() -> new ResourceNotFoundException("Destination branch not found"));
                    Product product = productRepository.findById(request.getProductId())
                            .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
                    newInv.setBranch(destBranch);
                    newInv.setProduct(product);
                    newInv.setAvailableStockKg(0.0);
                    newInv.setLowStockThreshold(5.0); // default
                    return newInv;
                });

        // Step 4: Deduct from source branch
        fromInventory.setAvailableStockKg(fromInventory.getAvailableStockKg() - request.getQuantity());
        fromInventory.setLastUpdated(LocalDateTime.now());

        // Step 5: Add to destination branch
        toInventory.setAvailableStockKg(toInventory.getAvailableStockKg() + request.getQuantity());
        toInventory.setLastUpdated(LocalDateTime.now());

        // Step 6: Save both — @Transactional ensures both save or neither saves
        branchInventoryRepository.save(fromInventory);
        branchInventoryRepository.save(toInventory);

        Map<String, Object> response = new HashMap<>();
        response.put("fromBranchId", request.getFromBranchId());
        response.put("toBranchId", request.getToBranchId());
        response.put("productId", request.getProductId());
        response.put("quantityTransferred", request.getQuantity());
        return response;
    }

    /**
     * BULK UPDATE stock across multiple entries.
     */
    @Transactional
    public List<BranchInventoryResponse> bulkUpdateStock(BulkStockUpdateRequest request) {
        List<BranchInventoryResponse> updatedList = new ArrayList<>();
        
        for (BulkStockUpdateRequest.StockUpdateItem item : request.getUpdates()) {
            BranchInventory inventory = branchInventoryRepository.findById(item.getInventoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Inventory not found with ID: " + item.getInventoryId()));
            
            inventory.setAvailableStockKg(item.getNewQuantity());
            inventory.setLastUpdated(LocalDateTime.now());
            
            BranchInventory saved = branchInventoryRepository.save(inventory);
            updatedList.add(mapToResponse(saved));
        }
        
        return updatedList;
    }

    /**
     * DECREMENT stock when a customer places an order.
     * Called by OrderService when order is confirmed.
     *
     * @param branchId  — which branch processed the order
     * @param productId — which product was ordered
     * @param quantityKg — how much was ordered
     */
    @Transactional
    public void decrementStock(Long branchId, Long productId, Double quantityKg) {

        // Find the stock entry for this branch + product
        BranchInventory inventory = branchInventoryRepository
                .findByBranch_IdAndProduct_Id(branchId, productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Stock entry not found for product: " + productId + " in branch: " + branchId));

        // Check if enough stock is available
        if (inventory.getAvailableStockKg() < quantityKg) {
            throw new RuntimeException("Insufficient stock. Available: " + inventory.getAvailableStockKg() + " kg");
        }

        // Deduct the ordered quantity
        inventory.setAvailableStockKg(inventory.getAvailableStockKg() - quantityKg);
        inventory.setLastUpdated(LocalDateTime.now());

        // Save updated stock
        branchInventoryRepository.save(inventory);
    }

    /**
     * Helper method — converts BranchInventory entity to BranchInventoryResponse DTO.
     * Also computes the lowStock flag automatically.
     */
    private BranchInventoryResponse mapToResponse(BranchInventory inv) {

        BranchInventoryResponse response = new BranchInventoryResponse();
        response.setId(inv.getId());

        // Branch info
        response.setBranchId(inv.getBranch().getId());
        response.setBranchName(inv.getBranch().getName());

        // Product info
        response.setProductId(inv.getProduct().getId());
        response.setProductName(inv.getProduct().getName());
        response.setPricePerKg(inv.getProduct().getPricePerKg());
        response.setImageUrl(inv.getProduct().getImageUrl());

        // Stock info
        response.setAvailableStockKg(inv.getAvailableStockKg());
        response.setLowStockThreshold(inv.getLowStockThreshold());

        // Compute lowStock flag — true if stock is at or below threshold
        boolean isLowStock = inv.getAvailableStockKg() <= inv.getLowStockThreshold();
        response.setLowStock(isLowStock);

        return response;
    }
}