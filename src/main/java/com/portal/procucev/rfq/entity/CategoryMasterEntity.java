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
@Table(name = "category_master", indexes = {
    @Index(name = "idx_cat_norm", columnList = "normalized_category")
})
public class CategoryMasterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "division", length = 255)
    private String division;

    @Column(name = "category", length = 255, nullable = false)
    private String category;

    @Column(name = "normalized_category", length = 255)
    private String normalizedCategory;

    @Builder.Default
    @Column(name = "active")
    private Boolean active = true;
}
