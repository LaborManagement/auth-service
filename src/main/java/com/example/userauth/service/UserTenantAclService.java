package com.example.userauth.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.userauth.controller.UserTenantAclController.BulkItem;
import com.example.userauth.controller.UserTenantAclController.BulkReplaceRequest;
import com.example.userauth.dao.UserTenantAclQueryDao;
import com.example.userauth.dto.EmployerAccessDto;
import com.example.userauth.dto.UserTenantAclDetailDto;
import com.example.userauth.entity.User;
import com.example.userauth.entity.UserRole;
import com.example.userauth.entity.UserTenantAcl;
import com.example.userauth.exception.BadRequestException;
import com.example.userauth.repository.UserTenantAclRepository;
import com.example.userauth.repository.UserRepository;

/**
 * Service for handling UserTenantAcl operations.
 * Provides enriched user tenant ACL details including ASSIGNED and PENDING
 * access.
 */
@Service
public class UserTenantAclService {

    private final UserTenantAclQueryDao userTenantAclQueryDao;
    private final UserRepository userRepository;
    private final UserTenantAclRepository userTenantAclRepository;

    public UserTenantAclService(UserTenantAclQueryDao userTenantAclQueryDao,
            UserRepository userRepository,
            UserTenantAclRepository userTenantAclRepository) {
        this.userTenantAclQueryDao = userTenantAclQueryDao;
        this.userRepository = userRepository;
        this.userTenantAclRepository = userTenantAclRepository;
    }

    /**
     * Get enriched user tenant ACL details for a specific user.
     * Returns both ASSIGNED (current access) and PENDING (available but not
     * assigned) access.
     * Query logic differs based on user role (MUKADAM_USER vs EMPLOYER).
     * 
     * @param userId The user ID to query
     * @return List of enriched UserTenantAclDetailDto objects with state field
     */
    @Transactional(readOnly = true)
    public List<UserTenantAclDetailDto> getUserTenantAclDetails(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        UserRole userRole = user.getRole();
        return userTenantAclQueryDao.getUserTenantAclDetails(userId, userRole);
    }

    @Transactional(readOnly = true)
    public List<EmployerAccessDto> getEmployersForUser(Long userId) {
        // validate user exists
        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        return userTenantAclQueryDao.getEmployersForUser(userId);
    }

    /**
     * Replace ACL rows for a user anchored by employer or toli.
     * Enforces employer_toli_relation constraints and the fixed anchor rule.
     */
    @Transactional
    public void bulkReplace(BulkReplaceRequest request) {
        String anchorType = request.anchorType().toUpperCase();
        if (!anchorType.equals("EMPLOYER") && !anchorType.equals("EMPLOYEE")) {
            throw new BadRequestException("anchorType must be EMPLOYER or EMPLOYEE");
        }

        Long anchorId = request.anchorId();
        List<BulkItem> items = request.items() == null ? List.of() : request.items();

        // Basic validation of payload
        for (BulkItem item : items) {
            if (item.boardId() == null) {
                throw new BadRequestException("boardId is required for each item");
            }
            if (anchorType.equals("EMPLOYER") && !anchorId.equals(item.employerId())) {
                throw new BadRequestException("All items must use the anchor employerId");
            }
            if (anchorType.equals("EMPLOYEE") && !anchorId.equals(item.toliId())) {
                throw new BadRequestException("All items must use the anchor toliId");
            }
        }

        // Allowed combos for this anchor from employer_toli_relation
        var allowedKeys = userTenantAclQueryDao.getAllowedKeysForAnchor(anchorType, anchorId);

        // Use sets for diff
        Set<String> desiredKeys = items.stream()
                .map(i -> key(i.boardId(), i.employerId(), i.toliId()))
                .collect(Collectors.toSet());

        List<UserTenantAcl> existing = anchorType.equals("EMPLOYER")
                ? userTenantAclRepository.findByUserIdAndEmployerId(request.userId(), anchorId)
                : userTenantAclRepository.findByUserIdAndToliId(request.userId(), anchorId);

        Set<String> existingKeys = existing.stream()
                .map(r -> key(r.getBoardId(), r.getEmployerId(), r.getToliId()))
                .collect(Collectors.toSet());

        // Delete rows no longer desired
        if (!existing.isEmpty()) {
            List<UserTenantAcl> toDelete = existing.stream()
                    .filter(r -> !desiredKeys.contains(key(r.getBoardId(), r.getEmployerId(), r.getToliId())))
                    .toList();
            userTenantAclRepository.deleteAll(toDelete);
        }

        // Add new rows or rows with changed perms
        for (BulkItem item : items) {
            String k = key(item.boardId(), item.employerId(), item.toliId());

            if (!allowedKeys.contains(k)) {
                throw new BadRequestException("Combination not allowed by employer_toli_relation: " + k);
            }
            boolean exists = existingKeys.contains(k);
            boolean canRead = item.canRead() != null ? item.canRead() : true;
            boolean canWrite = item.canWrite() != null ? item.canWrite() : false;
            if (canWrite && !canRead) canRead = true;

            if (exists) {
                // Update permissions if changed
                UserTenantAcl row = existing.stream()
                        .filter(r -> key(r.getBoardId(), r.getEmployerId(), r.getToliId()).equals(k))
                        .findFirst().orElseThrow();
                if (!row.getCanRead().equals(canRead) || !row.getCanWrite().equals(canWrite)) {
                    row.setCanRead(canRead);
                    row.setCanWrite(canWrite);
                    userTenantAclRepository.save(row);
                }
            } else {
                UserTenantAcl row = new UserTenantAcl();
                row.setUserId(request.userId());
                row.setBoardId(item.boardId());
                row.setEmployerId(item.employerId());
                row.setToliId(item.toliId());
                row.setCanRead(canRead);
                row.setCanWrite(canWrite);
                userTenantAclRepository.save(row);
            }
        }
    }

    private String key(Long boardId, Long employerId, Long toliId) {
        return boardId + "-" + (employerId == null ? "null" : employerId) + "-" + (toliId == null ? "null" : toliId);
    }
}
