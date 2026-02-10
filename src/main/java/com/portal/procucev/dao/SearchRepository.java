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
import java.util.List;

@Repository
@RequiredArgsConstructor
public class SearchRepository {

    @PersistenceContext
    private EntityManager em;

//    public List<BFSItemMainDetailsDTO> searchItems(List<String> keywords, int limit) {
//
//        List<String> cleanedKeywords = normalizeKeywords(keywords);
//        if (cleanedKeywords.isEmpty()) {
//            return Collections.emptyList();
//        }
//
//        // 1️⃣ Category search first
//        List<BFSItemMainDetailsDTO> categoryResults =
//                searchByCategory(cleanedKeywords, limit);
//
//        if (!categoryResults.isEmpty()) {
//            return categoryResults;
//        }
//
//        // 2️⃣ Fallback → description search
//        return searchByDescription(cleanedKeywords, limit);
//    }
//
//    /* ---------------------------------------------------
//       CATEGORY SEARCH
//     --------------------------------------------------- */
//    private List<BFSItemMainDetailsDTO> searchByCategory(
//            List<String> keywords, int limit) {
//
//        CriteriaBuilder cb = em.getCriteriaBuilder();
//        CriteriaQuery<BFSItemMainDetailsDTO> query =
//                cb.createQuery(BFSItemMainDetailsDTO.class);
//        Root<BFSItems> root = query.from(BFSItems.class);
//
//        List<Predicate> predicates = new ArrayList<>();
//        for (String keyword : keywords) {
//            predicates.add(
//                    cb.like(cb.lower(root.get("category")), "%" + keyword + "%")
//            );
//        }
//
//        query.select(buildDto(cb, root))
//             .where(cb.or(predicates.toArray(new Predicate[0])))
//             .orderBy(cb.asc(root.get("askPrice")));
//
//        return em.createQuery(query)
//                 .setMaxResults(limit)
//                 .getResultList();
//    }
//
//    /* ---------------------------------------------------
//       DESCRIPTION SEARCH (NO SPECIFICATION SEARCH)
//     --------------------------------------------------- */
//    private List<BFSItemMainDetailsDTO> searchByDescription(
//            List<String> keywords, int limit) {
//
//        CriteriaBuilder cb = em.getCriteriaBuilder();
//        CriteriaQuery<BFSItemMainDetailsDTO> query =
//                cb.createQuery(BFSItemMainDetailsDTO.class);
//        Root<BFSItems> root = query.from(BFSItems.class);
//
//        List<Predicate> predicates = new ArrayList<>();
//        for (String keyword : keywords) {
//            predicates.add(
//                    cb.like(cb.lower(root.get("description")), "%" + keyword + "%")
//            );
//        }
//
//        query.select(buildDto(cb, root))
//             .where(cb.or(predicates.toArray(new Predicate[0])))
//             .orderBy(cb.asc(root.get("askPrice")));
//
//        return em.createQuery(query)
//                 .setMaxResults(limit)
//                 .getResultList();
//    }
    public List<BFSItemMainDetailsDTO> searchItems(List<String> keywords, int limit) {

        List<String> cleanedKeywords = normalizeKeywords(keywords);

        // STEP 1 — combined search
        List<BFSItemMainDetailsDTO> results =
                searchCategoryAndDescription(cleanedKeywords, limit);

        if (results.size() >= limit) return results;

        List<String> excludeIds =
                results.stream().map(BFSItemMainDetailsDTO::getId).toList();

        // STEP 2 — category only
        int remaining = limit - results.size();
        List<BFSItemMainDetailsDTO> categoryOnly =
                searchCategoryOnly(cleanedKeywords, excludeIds, remaining);

        results.addAll(categoryOnly);

        if (results.size() >= limit) return results;

        excludeIds = results.stream().map(BFSItemMainDetailsDTO::getId).toList();

        // STEP 3 — description only
        remaining = limit - results.size();
        List<BFSItemMainDetailsDTO> descOnly =
                searchDescriptionOnly(cleanedKeywords, excludeIds, remaining);

        results.addAll(descOnly);

        return results;
    }

    private List<BFSItemMainDetailsDTO> searchDescriptionOnly(
            List<String> keywords, List<String> excludeIds, int limit) {

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<BFSItemMainDetailsDTO> query =
                cb.createQuery(BFSItemMainDetailsDTO.class);

        Root<BFSItems> root = query.from(BFSItems.class);

        List<Predicate> preds = new ArrayList<>();

        for (String keyword : keywords) {
            preds.add(cb.like(cb.lower(root.get("description")), "%" + keyword + "%"));
        }

        Predicate notIn = excludeIds.isEmpty()
                ? cb.conjunction()
                : cb.not(root.get("id").in(excludeIds));

        query.select(buildDto(cb, root))
             .where(cb.and(cb.or(preds.toArray(new Predicate[0])), notIn))
             .orderBy(cb.asc(root.get("askPrice")));

        return em.createQuery(query)
                .setMaxResults(limit)
                .getResultList();
    }

    
    private List<BFSItemMainDetailsDTO> searchCategoryAndDescription(
            List<String> keywords, int limit) {

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<BFSItemMainDetailsDTO> query =
                cb.createQuery(BFSItemMainDetailsDTO.class);

        Root<BFSItems> root = query.from(BFSItems.class);

        List<Predicate> preds = new ArrayList<>();

        for (String keyword : keywords) {
            String like = "%" + keyword + "%";

            Predicate cat = cb.like(cb.lower(root.get("category")), like);
            Predicate desc = cb.like(cb.lower(root.get("description")), like);

            preds.add(cb.or(cat, desc));
        }

        query.select(buildDto(cb, root))
             .where(cb.and(preds.toArray(new Predicate[0])))
             .orderBy(cb.asc(root.get("askPrice")));

        return em.createQuery(query)
                .setMaxResults(limit)
                .getResultList();
    }

    private List<BFSItemMainDetailsDTO> searchCategoryOnly(
            List<String> keywords, List<String> excludeIds, int limit) {

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<BFSItemMainDetailsDTO> query =
                cb.createQuery(BFSItemMainDetailsDTO.class);

        Root<BFSItems> root = query.from(BFSItems.class);

        List<Predicate> preds = new ArrayList<>();

        for (String keyword : keywords) {
            preds.add(cb.like(cb.lower(root.get("category")), "%" + keyword + "%"));
        }

        Predicate notIn = excludeIds.isEmpty()
                ? cb.conjunction()
                : cb.not(root.get("id").in(excludeIds));

        query.select(buildDto(cb, root))
             .where(cb.and(cb.or(preds.toArray(new Predicate[0])), notIn))
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

    /* ---------------------------------------------------
       KEYWORD NORMALIZATION
     --------------------------------------------------- */
    private List<String> normalizeKeywords(List<String> keywords) {
        if (keywords == null) return Collections.emptyList();

        return keywords.stream()
                .filter(k -> k != null && !k.trim().isEmpty())
                .map(String::toLowerCase)
                .filter(k -> !k.equals("others") && !k.equals("other"))
                .distinct()
                .toList();
    }
}
