package com.portal.procucev.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import com.portal.procucev.Dto.BFSItemMainDetailsDTO;
import com.portal.procucev.model.BFSItems;

import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class SearchRepository {

    @PersistenceContext
    private final EntityManager em;

    public List<BFSItemMainDetailsDTO> searchItems(List<String> keywords, int limit) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<BFSItemMainDetailsDTO> query = cb.createQuery(BFSItemMainDetailsDTO.class);
        Root<BFSItems> root = query.from(BFSItems.class);

        List<Predicate> orPredicates = new ArrayList<>();
        for (String keyword : keywords) {
            String likeExpr = "%" + keyword.toLowerCase() + "%";
            orPredicates.add(cb.like(cb.lower(root.get("description")), likeExpr));
            orPredicates.add(cb.like(cb.lower(root.get("category")), likeExpr));
        }

        query.select(cb.construct(
                BFSItemMainDetailsDTO.class,
                root.get("id"),
                root.get("description"),
                root.get("specification"),
                root.get("totalQuantity"),
                root.get("availableQuantity"),
                root.get("category"),
                root.get("itemNumber"),
                root.get("location"),
                root.get("ageOfAsset"),
                root.get("unitofMeasures"),
                root.get("sellPrice"),
                root.get("discount"),
                root.get("askPrice"),
                root.get("bfsGroup"),
                root.get("buyPriceDisclosure"),
                root.get("remarks"),
                root.get("imagesFlag")
        ))
        .where(cb.or(orPredicates.toArray(new Predicate[0])))
        .orderBy(cb.asc(root.get("askPrice")));

        return em.createQuery(query)
                 .setMaxResults(limit)
                 .getResultList();
    }
}
