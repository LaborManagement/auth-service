package com.example.userauth.repository;

import com.example.userauth.entity.UIPage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for UIPage entity.
 * Provides methods to query UI pages and navigation structure.
 */
@Repository
public interface UIPageRepository extends JpaRepository<UIPage, Long> {

    /**
     * Find a UI page by its unique key
     */
    Optional<UIPage> findByKey(String key);

    /**
     * Find all pages for a given module
     */
    List<UIPage> findByModule(String module);

    /**
     * Find all active pages
     */
    List<UIPage> findByIsActiveTrue();

    /**
     * Find all active menu items (for navigation)
     */
    List<UIPage> findByIsMenuItemTrueAndIsActiveTrueOrderByDisplayOrder();

    /**
     * Find all child pages of a parent page
     */
    List<UIPage> findByParentIdAndIsActiveTrueOrderByDisplayOrder(Long parentId);
    
    /**
     * Find all child pages of a parent (including inactive)
     */
    List<UIPage> findByParentId(Long parentId);

    /**
     * Find all root-level pages (no parent)
     */
    List<UIPage> findByParentIdIsNullAndIsMenuItemTrueAndIsActiveTrueOrderByDisplayOrder();

    /**
     * Find active pages for a specific module
     */
    List<UIPage> findByModuleAndIsActiveTrueOrderByDisplayOrder(String module);

    /**
     * Check if a page exists by key
     */
    boolean existsByKey(String key);

    /**
     * Find all pages used to build the navigation tree.
     */
    @Query("SELECT p FROM UIPage p " +
           "WHERE p.isActive = true " +
           "AND p.isMenuItem = true " +
           "ORDER BY p.parentId, p.displayOrder")
    List<UIPage> findAllMenuPages();
    
    /**
     * Find all active pages ordered by display order
     */
    List<UIPage> findByIsActiveTrueOrderByDisplayOrderAsc();
}
