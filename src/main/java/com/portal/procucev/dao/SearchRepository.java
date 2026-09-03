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
import com.portal.procucev.Dto.VendorRFQDto;
import com.portal.procucev.model.BFSItems;
import com.portal.procucev.model.OrgType;
import com.portal.procucev.model.Organization;

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

    public List<VendorRFQDto> searchVendorsByMultipleValues(
            OrgType orgType, String searchType, List<String> searchValues) {
        if (searchValues == null || searchValues.isEmpty()) {
            return Collections.emptyList();
        }
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<VendorRFQDto> cq = cb.createQuery(VendorRFQDto.class);
        Root<Organization> root = cq.from(Organization.class);

        cq.select(cb.construct(
                VendorRFQDto.class,
                root.get("id"),
                root.get("companyName"),
                root.get("companyId"),
                root.get("organizationPhonenumber"),
                root.get("city"),
                root.get("email")
        ));

        List<Predicate> valuePredicates = new ArrayList<>();
        String cleanType = searchType != null ? searchType.trim() : "";

        for (String val : searchValues) {
            if (val == null || val.trim().isEmpty()) continue;
            String pattern = "%" + val.trim().toLowerCase() + "%";

            if ("email".equalsIgnoreCase(cleanType)) {
                Predicate p1 = cb.like(cb.lower(root.get("email")), pattern);
                Predicate p2 = cb.like(cb.lower(root.get("otherEmails")), pattern);
                valuePredicates.add(cb.or(p1, p2));
            } else if ("mobileNumber".equalsIgnoreCase(cleanType)) {
                valuePredicates.add(cb.like(cb.lower(root.get("organizationPhonenumber")), pattern));
            } else if ("sellerName".equalsIgnoreCase(cleanType) || "vendorName".equalsIgnoreCase(cleanType) || "companyName".equalsIgnoreCase(cleanType)) {
                valuePredicates.add(cb.like(cb.lower(root.get("companyName")), pattern));
            } else if ("city".equalsIgnoreCase(cleanType)) {
                valuePredicates.add(cb.like(cb.lower(root.get("city")), pattern));
            } else {
                // All search criteria
                Predicate p1 = cb.like(cb.lower(root.get("email")), pattern);
                Predicate p2 = cb.like(cb.lower(root.get("otherEmails")), pattern);
                Predicate p3 = cb.like(cb.lower(root.get("companyName")), pattern);
                Predicate p4 = cb.like(cb.lower(root.get("organizationPhonenumber")), pattern);
                Predicate p5 = cb.like(cb.lower(root.get("city")), pattern);
                valuePredicates.add(cb.or(p1, p2, p3, p4, p5));
            }
        }

        if (valuePredicates.isEmpty()) {
            return Collections.emptyList();
        }

        Predicate mainPredicate = cb.and(
                cb.equal(root.get("orgType"), orgType),
                cb.or(valuePredicates.toArray(new Predicate[0]))
        );

        cq.where(mainPredicate);
        cq.orderBy(cb.desc(root.get("createdTS")));

        return em.createQuery(cq).getResultList();
    }
}
