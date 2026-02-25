package com.example.userauth.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.example.userauth.dto.EmployerAccessDto;
import com.example.userauth.dto.UserTenantAclDetailDto;
import com.example.userauth.entity.UserRole;

/**
 * Data Access Object for UserTenantAcl complex queries.
 * Uses Spring JdbcTemplate for UNION queries with ASSIGNED and PENDING states.
 * 
 * Pattern: Direct SQL for complex UNION queries
 */
@Repository
public class UserTenantAclQueryDao {

    private final JdbcTemplate jdbcTemplate;

    public UserTenantAclQueryDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Allowed employer/toli combos for an anchor.
     * EMPLOYER anchor -> rows with that employer_id; EMPLOYEE anchor -> rows with that toli_id.
     */
    public Set<String> getAllowedKeysForAnchor(String anchorType, Long anchorId) {
        String sql;
        Object[] args;
        if ("EMPLOYER".equalsIgnoreCase(anchorType)) {
            sql = "SELECT board_id, employer_id, toli_id FROM payment_flow.employer_toli_relation WHERE employer_id = ?";
            args = new Object[] { anchorId };
        } else {
            sql = "SELECT board_id, employer_id, toli_id FROM payment_flow.employer_toli_relation WHERE toli_id = ?";
            args = new Object[] { anchorId };
        }

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, args);
        return rows.stream()
                .map(row -> {
                    Long b = ((Number) row.get("board_id")).longValue();
                    Long e = ((Number) row.get("employer_id")).longValue();
                    Long t = ((Number) row.get("toli_id")).longValue();
                    return key(b, e, t);
                })
                .collect(Collectors.toSet());
    }

    private String key(Long boardId, Long employerId, Long toliId) {
        return boardId + "-" + employerId + "-" + toliId;
    }

    /**
     * Get enriched user tenant ACL details for a specific user.
     * Returns both ASSIGNED (current access) and PENDING (available but not
     * assigned) tolis.
     * Query differs based on user role (MUKADAM_USER vs EMPLOYER).
     * 
     * @param userId   The user ID to query
     * @param userRole The user's role
     * @return List of enriched UserTenantAclDetailDto objects with state field
     */
    public List<UserTenantAclDetailDto> getUserTenantAclDetails(Long userId, UserRole userRole) {
        String sql = getUserTenantAclQuery(userRole);
        return jdbcTemplate.query(sql, new UserTenantAclDetailRowMapper(), userId, userId);
    }

    /**
     * Employers accessible to the given user, restricted to employer/toli combinations
     * that exist in employer_toli_relation. Mirrors UI expectation for employer dropdown.
     */
    public List<EmployerAccessDto> getEmployersForUser(Long userId) {
        String sql = """
                SELECT e.establishment_name,
                       e.registration_number,
                       a.employer_id
                FROM auth.user_tenant_acl a
                JOIN payment_flow.employer_master e ON a.employer_id = e.id
                WHERE a.user_id = ?
                  AND EXISTS (
                      SELECT 1
                      FROM payment_flow.employer_toli_relation etr
                      WHERE etr.toli_id = a.toli_id
                        AND etr.employer_id = a.employer_id
                  )
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new EmployerAccessDto(
                rs.getLong("employer_id"),
                rs.getString("establishment_name"),
                rs.getString("registration_number")),
                userId);
    }

    /**
     * Returns appropriate SQL query based on user role.
     * WORKER: Join etr by toli_id (get employers for same toli)
     * EMPLOYER: Join etr by employer_id (get tolis for same employer)
     */
    private String getUserTenantAclQuery(UserRole userRole) {
        if (userRole == UserRole.WORKER) {
            // Employee type - get employers for the same toli
            return """
                    SELECT 'ASSIGNED' AS state,
                           m.id AS user_id,
                           m.username,
                           e.id AS employer_id,
                           e.registration_number AS employer_reg_no,
                           e.establishment_name,
                           t.id AS toli_id,
                           t.registration_number AS toli_reg_no,
                           t.employer_name_english,
                           u.can_read,
                           u.can_write
                    FROM auth.user_tenant_acl u
                    JOIN auth.users m ON u.user_id = m.id
                    JOIN payment_flow.employer_master e ON u.employer_id = e.id
                    JOIN payment_flow.toli_master t ON u.toli_id = t.id
                    WHERE m.id = ?
                    UNION
                    SELECT 'PENDING' AS state,
                           m.id AS user_id,
                           m.username,
                           etr.employer_id AS employer_id,
                           e.registration_number AS employer_reg_no,
                           e.establishment_name,
                           etr.toli_id AS toli_id,
                           t.registration_number AS toli_reg_no,
                           t.employer_name_english,
                           false AS can_read,
                           false AS can_write
                    FROM auth.user_tenant_acl u
                    JOIN auth.users m ON u.user_id = m.id
                    JOIN payment_flow.employer_toli_relation etr ON etr.toli_id = u.toli_id
                    JOIN payment_flow.employer_master e ON e.id = etr.employer_id
                    JOIN payment_flow.toli_master t ON t.id = etr.toli_id
                    WHERE m.id = ?
                      AND NOT EXISTS (
                        SELECT 1
                        FROM auth.user_tenant_acl u2
                        WHERE u2.user_id = m.id
                          AND u2.employer_id = etr.employer_id
                          AND u2.toli_id = etr.toli_id
                      )
                    ORDER BY state, employer_id, toli_id
                    """;
        } else {
            // Employer type - get tolis for the same employer
            return """
                    SELECT 'ASSIGNED' AS state,
                           m.id AS user_id,
                           m.username,
                           e.id AS employer_id,
                           e.registration_number AS employer_reg_no,
                           e.establishment_name,
                           t.id AS toli_id,
                           t.registration_number AS toli_reg_no,
                           t.employer_name_english,
                           u.can_read,
                           u.can_write
                    FROM auth.user_tenant_acl u
                    JOIN auth.users m ON u.user_id = m.id
                    JOIN payment_flow.employer_master e ON u.employer_id = e.id
                    JOIN payment_flow.toli_master t ON u.toli_id = t.id
                    WHERE m.id = ?
                    UNION
                    SELECT 'PENDING' AS state,
                           m.id AS user_id,
                           m.username,
                           etr.employer_id AS employer_id,
                           e.registration_number AS employer_reg_no,
                           e.establishment_name,
                           etr.toli_id AS toli_id,
                           t.registration_number AS toli_reg_no,
                           t.employer_name_english,
                           false AS can_read,
                           false AS can_write
                    FROM auth.user_tenant_acl u
                    JOIN auth.users m ON u.user_id = m.id
                    JOIN payment_flow.employer_toli_relation etr ON etr.employer_id = u.employer_id
                    JOIN payment_flow.employer_master e ON e.id = etr.employer_id
                    JOIN payment_flow.toli_master t ON t.id = etr.toli_id
                    WHERE m.id = ?
                      AND NOT EXISTS (
                        SELECT 1
                        FROM auth.user_tenant_acl u2
                        WHERE u2.user_id = m.id
                          AND u2.employer_id = etr.employer_id
                          AND u2.toli_id = etr.toli_id
                      )
                    ORDER BY state, employer_id, toli_id
                    """;
        }
    }

    /**
     * RowMapper for UserTenantAclDetailDto including state field.
     */
    private static class UserTenantAclDetailRowMapper implements RowMapper<UserTenantAclDetailDto> {
        @Override
        public UserTenantAclDetailDto mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new UserTenantAclDetailDto(
                    rs.getString("state"),
                    rs.getLong("user_id"),
                    rs.getString("username"),
                    rs.getLong("employer_id"),
                    rs.getString("employer_reg_no"),
                    rs.getString("establishment_name"),
                    rs.getLong("toli_id"),
                    rs.getString("toli_reg_no"),
                    rs.getString("employer_name_english"),
                    rs.getBoolean("can_read"),
                    rs.getBoolean("can_write"));
        }
    }
}
