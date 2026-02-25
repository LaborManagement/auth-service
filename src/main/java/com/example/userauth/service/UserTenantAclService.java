package com.example.userauth.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.userauth.dao.UserTenantAclQueryDao;
import com.example.userauth.dto.EmployerAccessDto;
import com.example.userauth.dto.UserTenantAclDetailDto;
import com.example.userauth.entity.User;
import com.example.userauth.entity.UserRole;
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

    public UserTenantAclService(UserTenantAclQueryDao userTenantAclQueryDao, UserRepository userRepository) {
        this.userTenantAclQueryDao = userTenantAclQueryDao;
        this.userRepository = userRepository;
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
}
