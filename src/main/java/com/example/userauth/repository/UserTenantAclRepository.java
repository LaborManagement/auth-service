package com.example.userauth.repository;

import com.example.userauth.entity.UserTenantAcl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing user tenant ACL records.
 * Maps users to their allowed (board, employer) combinations.
 */
@Repository
public interface UserTenantAclRepository extends JpaRepository<UserTenantAcl, Long> {
    
    /**
     * Find all ACL records for a specific user.
     */
    List<UserTenantAcl> findByUserId(Long userId);
    
    /**
     * Find ACL record for a user accessing a specific board and employer.
     */
    Optional<UserTenantAcl> findByUserIdAndBoardIdAndEmployerId(Long userId, Long boardId, Long employerId);
    
    /**
     * Find all ACL records for a specific board.
     */
    List<UserTenantAcl> findByBoardId(Long boardId);
    
    /**
     * Find all ACL records for a specific employer.
     */
    List<UserTenantAcl> findByEmployerId(Long employerId);
    
    /**
     * Find all ACL records for a user accessing a specific board (any employer).
     */
    List<UserTenantAcl> findByUserIdAndBoardId(Long userId, Long boardId);
    
    /**
     * Delete all ACL records for a user.
     */
    void deleteByUserId(Long userId);
    
    /**
     * Check if user has read access to a board/employer combination.
     */
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END " +
           "FROM UserTenantAcl a WHERE a.userId = :userId AND a.boardId = :boardId " +
           "AND ((:employerId IS NULL AND a.employerId IS NULL) OR a.employerId = :employerId OR a.employerId IS NULL) AND a.canRead = true")
    boolean hasReadAccess(@Param("userId") Long userId, @Param("boardId") Long boardId, @Param("employerId") Long employerId);
    
    /**
     * Check if user has write access to a board/employer combination.
     */
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END " +
           "FROM UserTenantAcl a WHERE a.userId = :userId AND a.boardId = :boardId " +
           "AND ((:employerId IS NULL AND a.employerId IS NULL) OR a.employerId = :employerId OR a.employerId IS NULL) AND a.canWrite = true")
    boolean hasWriteAccess(@Param("userId") Long userId, @Param("boardId") Long boardId, @Param("employerId") Long employerId);
    
    /**
     * Count total ACL records for a user.
     */
    long countByUserId(Long userId);
}
