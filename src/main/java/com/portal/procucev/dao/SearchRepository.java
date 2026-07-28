package com.portal.procucev.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import com.portal.procucev.Dto.BFSItemMainDetailsDTO;
import com.portal.procucev.model.BFSItems;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class SearchRepository {

    @PersistenceContext
    private EntityManager em;

    public List<BFSItemMainDetailsDTO> searchItems(
            Set<String> categoryKeywords, Set<String> descriptionKeywords, int limit) {

        if ((categoryKeywords == null || categoryKeywords.isEmpty()) &&
            (descriptionKeywords == null || descriptionKeywords.isEmpty())) {
            return Collections.emptyList();
        }

        List<BFSItemMainDetailsDTO> results = new ArrayList<>();
        Set<String> selectedIds = new HashSet<>();

        // 1. Category + Description matches (highest priority)
        List<BFSItemMainDetailsDTO> catDescMatches = searchCategoryAndDescription(
                categoryKeywords, descriptionKeywords, selectedIds, limit
        );
        results.addAll(catDescMatches);
        selectedIds.addAll(catDescMatches.stream().map(BFSItemMainDetailsDTO::getId).toList());

        if (results.size() >= limit) return results.subList(0, limit);

        int remaining = limit - results.size();

        // 2. Description-only matches (fill remaining)
        List<BFSItemMainDetailsDTO> descMatches = searchDescriptionOnly(
                descriptionKeywords, selectedIds, remaining
        );
        results.addAll(descMatches);
        selectedIds.addAll(descMatches.stream().map(BFSItemMainDetailsDTO::getId).toList());

        if (results.size() >= limit) return results.subList(0, limit);

        remaining = limit - results.size();

        // 3. Category-only matches (fill remaining)
        List<BFSItemMainDetailsDTO> catMatches = searchCategoryOnly(
                categoryKeywords, selectedIds, remaining
        );
        results.addAll(catMatches);

        return results.subList(0, Math.min(limit, results.size()));
    }

    /* ---------------------- SEARCH HELPERS ---------------------- */

    private List<BFSItemMainDetailsDTO> searchCategoryAndDescription(
            Set<String> categoryKeywords,
            Set<String> descriptionKeywords,
            Set<String> excludeIds,
            int limit) {

        if ((categoryKeywords == null || categoryKeywords.isEmpty()) ||
            (descriptionKeywords == null || descriptionKeywords.isEmpty())) {
            return Collections.emptyList();
        }

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<BFSItemMainDetailsDTO> query = cb.createQuery(BFSItemMainDetailsDTO.class);
        Root<BFSItems> root = query.from(BFSItems.class);

        // Category predicates (OR)
        List<Predicate> catPreds = new ArrayList<>();
        for (String keyword : categoryKeywords) {
            catPreds.add(cb.like(cb.lower(root.get("category")), "%" + keyword.toLowerCase() + "%"));
        }
        Predicate catPredicate = cb.or(catPreds.toArray(new Predicate[0]));

        // Description predicates (OR)
        List<Predicate> descPreds = new ArrayList<>();
        for (String keyword : descriptionKeywords) {
            descPreds.add(cb.like(cb.lower(root.get("description")), "%" + keyword.toLowerCase() + "%"));
        }
        Predicate descPredicate = cb.or(descPreds.toArray(new Predicate[0]));

        // Combine category AND description
        Predicate combined = cb.and(catPredicate, descPredicate);

        // Exclude already selected IDs
        Predicate notIn = excludeIds.isEmpty() ? cb.conjunction() : cb.not(root.get("id").in(excludeIds));

        query.select(buildDto(cb, root))
             .where(cb.and(combined, notIn))
             .orderBy(cb.asc(root.get("askPrice")));

        return em.createQuery(query)
                 .setMaxResults(limit)
                 .getResultList();
    }

    private List<BFSItemMainDetailsDTO> searchCategoryOnly(
            Set<String> categoryKeywords, Set<String> excludeIds, int limit) {

        if (categoryKeywords == null || categoryKeywords.isEmpty()) return Collections.emptyList();

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<BFSItemMainDetailsDTO> query = cb.createQuery(BFSItemMainDetailsDTO.class);
        Root<BFSItems> root = query.from(BFSItems.class);

        List<Predicate> catPreds = new ArrayList<>();
        for (String keyword : categoryKeywords) {
            catPreds.add(cb.like(cb.lower(root.get("category")), "%" + keyword.toLowerCase() + "%"));
        }

        Predicate notIn = excludeIds.isEmpty() ? cb.conjunction() : cb.not(root.get("id").in(excludeIds));

        query.select(buildDto(cb, root))
             .where(cb.and(cb.or(catPreds.toArray(new Predicate[0])), notIn))
             .orderBy(cb.asc(root.get("askPrice")));

        return em.createQuery(query)
                 .setMaxResults(limit)
                 .getResultList();
    }

    private List<BFSItemMainDetailsDTO> searchDescriptionOnly(
            Set<String> descriptionKeywords, Set<String> excludeIds, int limit) {

        if (descriptionKeywords == null || descriptionKeywords.isEmpty()) return Collections.emptyList();

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<BFSItemMainDetailsDTO> query = cb.createQuery(BFSItemMainDetailsDTO.class);
        Root<BFSItems> root = query.from(BFSItems.class);

        List<Predicate> descPreds = new ArrayList<>();
        for (String keyword : descriptionKeywords) {
            descPreds.add(cb.like(cb.lower(root.get("description")), "%" + keyword.toLowerCase() + "%"));
        }

        Predicate notIn = excludeIds.isEmpty() ? cb.conjunction() : cb.not(root.get("id").in(excludeIds));

        query.select(buildDto(cb, root))
             .where(cb.and(cb.or(descPreds.toArray(new Predicate[0])), notIn))
             .orderBy(cb.asc(root.get("askPrice")));

        return em.createQuery(query)
                 .setMaxResults(limit)
                 .getResultList();
    }


    /* ---------------------------------------------------
       DTO CONSTRUCTION
     --------------------------------------------------- */
    private Selection<BFSItemMainDetailsDTO> buildDto(
            CriteriaBuilder cb, Root<BFSItems> root) {

        return cb.construct(
                BFSItemMainDetailsDTO.class,
                root.get("id"),
                root.get("description"),
                root.get("specification"),      // ✅ included
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
        );
    }
}
