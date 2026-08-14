package com.portal.procucev.dao;

import com.portal.procucev.Dto.BFSItemMainDetailsDTO;
import com.portal.procucev.model.BFSItems;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SearchRepositoryTest {

    @Mock
    private EntityManager em;
    @Mock
    private CriteriaBuilder cb;
    @Mock
    private CriteriaQuery<BFSItemMainDetailsDTO> query;
    @Mock
    private Root<BFSItems> root;
    @Mock
    private TypedQuery<BFSItemMainDetailsDTO> typedQuery;
    @Mock
    private Predicate predicate;
    @Mock
    private Path<Object> path;
    @Mock
    private CompoundSelection<BFSItemMainDetailsDTO> selection;
    @Mock
    private Expression<String> expression;

    @InjectMocks
    private SearchRepository searchRepository;

    @BeforeEach
    void setUp() {
        when(em.getCriteriaBuilder()).thenReturn(cb);
        when(cb.createQuery(BFSItemMainDetailsDTO.class)).thenReturn(query);
        when(query.from(BFSItems.class)).thenReturn(root);
        when(query.select(any())).thenReturn(query);
        when(query.where(any(Predicate.class))).thenReturn(query);
        when(query.orderBy(any(Order[].class))).thenReturn(query);
        when(query.orderBy(any(List.class))).thenReturn(query);
        when(cb.like(any(), anyString())).thenReturn(predicate);
        when(cb.lower(any())).thenReturn(expression);
        when(root.get(anyString())).thenReturn(path);
        when(cb.or(any(Predicate[].class))).thenReturn(predicate);
        when(cb.and(any(Predicate[].class))).thenReturn(predicate);
        when(cb.and(any(Predicate.class), any(Predicate.class))).thenReturn(predicate);
        when(cb.conjunction()).thenReturn(predicate);
        when(cb.not(any())).thenReturn(predicate);
        when(cb.asc(any())).thenReturn(null);
        when(cb.construct(eq(BFSItemMainDetailsDTO.class), any())).thenReturn(selection);

        when(em.createQuery(query)).thenReturn(typedQuery);
        when(typedQuery.setMaxResults(anyInt())).thenReturn(typedQuery);
    }

    @Test
    void testSearchItems_NullOrEmptyKeywords() {
        assertTrue(searchRepository.searchItems(null, null, 10).isEmpty());
        assertTrue(searchRepository.searchItems(Collections.emptySet(), Collections.emptySet(), 10).isEmpty());
        assertTrue(searchRepository.searchItems(Set.of(), null, 10).isEmpty());
        assertTrue(searchRepository.searchItems(null, Set.of(), 10).isEmpty());
        assertTrue(searchRepository.searchItems(Set.of(), Set.of(), 10).isEmpty());
    }

    @Test
    void testSearchItems_CategoryAndDescription_FullFlow() {
        Set<String> cat = Set.of("Category1", "Category2");
        Set<String> desc = Set.of("Desc1", "Desc2");

        BFSItemMainDetailsDTO dto1 = new BFSItemMainDetailsDTO();
        dto1.setId("ID1");

        // 1st query returns 1 item, 2nd query returns 1 item, 3rd query returns 1 item
        when(typedQuery.getResultList())
                .thenReturn(new ArrayList<>(List.of(dto1)))
                .thenReturn(new ArrayList<>(List.of(dto1)))
                .thenReturn(new ArrayList<>(List.of(dto1)));

        List<BFSItemMainDetailsDTO> res = searchRepository.searchItems(cat, desc, 10);
        assertNotNull(res);
        assertFalse(res.isEmpty());
    }

    @Test
    void testSearchItems_CategoryOnly_And_DescriptionOnly_IndividualBranches() {
        Set<String> cat = Set.of("Cat1");
        Set<String> desc = Set.of("Desc1");

        BFSItemMainDetailsDTO dto1 = new BFSItemMainDetailsDTO();
        dto1.setId("ID1");
        when(typedQuery.getResultList()).thenReturn(new ArrayList<>(List.of(dto1)));

        // Test Category Only with null/empty/valid description
        List<BFSItemMainDetailsDTO> res1 = searchRepository.searchItems(cat, null, 10);
        assertNotNull(res1);

        List<BFSItemMainDetailsDTO> res2 = searchRepository.searchItems(cat, Collections.emptySet(), 10);
        assertNotNull(res2);

        // Test Description Only with null/empty/valid category
        List<BFSItemMainDetailsDTO> res3 = searchRepository.searchItems(null, desc, 10);
        assertNotNull(res3);

        List<BFSItemMainDetailsDTO> res4 = searchRepository.searchItems(Collections.emptySet(), desc, 10);
        assertNotNull(res4);
    }

    @Test
    void testSearchItems_LimitReachedAfterDescriptionOnlyPass() {
        Set<String> cat = Set.of("Cat1");
        Set<String> desc = Set.of("Desc1");

        BFSItemMainDetailsDTO first = new BFSItemMainDetailsDTO();
        first.setId("ID1");
        BFSItemMainDetailsDTO second = new BFSItemMainDetailsDTO();
        second.setId("ID2");
        BFSItemMainDetailsDTO third = new BFSItemMainDetailsDTO();
        third.setId("ID3");

        // pass 1 leaves room, pass 2 fills the quota so pass 3 is never issued
        when(typedQuery.getResultList())
                .thenReturn(new ArrayList<>(List.of(first)))
                .thenReturn(new ArrayList<>(List.of(second, third)))
                .thenReturn(new ArrayList<>(List.of(first, second, third)));

        List<BFSItemMainDetailsDTO> res = searchRepository.searchItems(cat, desc, 3);
        assertEquals(3, res.size());
        verify(typedQuery, times(2)).getResultList();
    }

    @Test
    void testSearchCategoryAndDescription_ExcludesAlreadySelectedIds() {
        when(path.in(anyCollection())).thenReturn(predicate);
        BFSItemMainDetailsDTO dto = new BFSItemMainDetailsDTO();
        dto.setId("ID9");
        when(typedQuery.getResultList()).thenReturn(new ArrayList<>(List.of(dto)));

        List<BFSItemMainDetailsDTO> res = ReflectionTestUtils.invokeMethod(
                searchRepository, "searchCategoryAndDescription",
                Set.of("Cat1"), Set.of("Desc1"), new HashSet<>(Set.of("ALREADY_PICKED")), 5);

        assertEquals(1, res.size());
        verify(cb).not(any());
    }

    @Test
    void testSearchItems_LimitCap() {
        Set<String> cat = Set.of("Cat1");
        Set<String> desc = Set.of("Desc1");

        BFSItemMainDetailsDTO dto1 = new BFSItemMainDetailsDTO();
        dto1.setId("ID1");
        BFSItemMainDetailsDTO dto2 = new BFSItemMainDetailsDTO();
        dto2.setId("ID2");
        when(typedQuery.getResultList()).thenReturn(new ArrayList<>(List.of(dto1, dto2)));

        List<BFSItemMainDetailsDTO> res = searchRepository.searchItems(cat, desc, 1);
        assertEquals(1, res.size());
    }
}
