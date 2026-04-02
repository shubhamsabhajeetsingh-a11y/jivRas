package com.jivRas.groceries.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * BranchInventory — This is the CORE of multi-branch stock management.
 *
 * Think of it like this:
 *   Product table  = Global product catalog (name, price, image) — same across all branches
 *   BranchInventory = Stock of that product IN a specific branch
 *
 * Example:
 *   Basmati Rice → Virar Branch   → 50 kg
 *   Basmati Rice → Vasai Branch   → 30 kg
 *   Basmati Rice → Nalasopara     →  0 kg (out of stock there)
 *
 * UniqueConstraint ensures one product cannot have two stock entries for same branch.
 */
@Entity
@Table(
    name = "branch_inventory",
    uniqueConstraints = @UniqueConstraint(columnNames = {"branch_id", "product_id"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BranchInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Which branch this stock belongs to
    @ManyToOne
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    // Which product this stock entry is for
    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // Current available stock in kg for this product at this branch
    @Column(nullable = false)
    private Double availableStockKg = 0.0;

    // If stock falls below this number → Low Stock Alert triggered
    // Default 5 kg — can be changed per product per branch
    @Column(nullable = false)
    private Double lowStockThreshold = 5.0;

    // Last time stock was updated — useful for audit trail
    private LocalDateTime lastUpdated;
}