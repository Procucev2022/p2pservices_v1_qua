package com.portal.procucev.rfq.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "category_item_mapping", indexes = {
    @Index(name = "idx_norm_desc", columnList = "normalized_description"),
    @Index(name = "idx_cat_norm_mapping", columnList = "normalized_category")
})
public class CategoryItemMappingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "division", length = 255)
    private String division;

    @Column(name = "category", length = 255, nullable = false)
    private String category;

    @Column(name = "normalized_category", length = 255)
    private String normalizedCategory;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "normalized_description", length = 1000)
    private String normalizedDescription;
}
