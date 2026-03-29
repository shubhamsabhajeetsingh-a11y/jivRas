package com.jivRas.groceries.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "Product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;            // Jaggery, Makhana
    private String description;

    private double pricePerKg;

    private double availableStockKg;   // 200 kg

    private String imageUrl;            // stored image path

    private boolean active = true;

    @jakarta.persistence.ManyToOne
    @jakarta.persistence.JoinColumn(name = "category_id")
    private Category category;
}
