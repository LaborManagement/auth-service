package com.example.userauth.dao;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import com.example.userauth.entity.User;
import com.example.userauth.entity.UserRole;

/**
 * Read-optimised access to the {@code users} table leveraging jOOQ for
 * type-safe SQL construction.
 */
@Repository
public class UserQueryDao {

    private static final Table<Record> USERS = DSL.table(DSL.name("users"));
    private static final Table<Record> USER_ROLES = DSL.table(DSL.name("user_roles"));
    private static final Table<Record> ROLES = DSL.table(DSL.name("roles"));
    private static final Table<Record> ROLE_POLICIES = DSL.table(DSL.name("role_policies"));
    private static final Table<Record> POLICIES = DSL.table(DSL.name("policies"));
    private static final Table<Record> USER_TENANT_ACL = DSL.table(DSL.name("user_tenant_acl"));

    private static final Field<Long> U_ID = DSL.field(DSL.name("u", "id"), Long.class);
    private static final Field<String> U_USERNAME = DSL.field(DSL.name("u", "username"), String.class);
    private static final Field<String> U_EMAIL = DSL.field(DSL.name("u", "email"), String.class);
    private static final Field<String> U_FULL_NAME = DSL.field(DSL.name("u", "full_name"), String.class);
    private static final Field<String> U_ROLE = DSL.field(DSL.name("u", "role"), String.class);
    private static final Field<String> U_USER_TYPE = DSL.field(DSL.name("u", "user_type"), String.class);
    private static final Field<String> U_AUTH_LEVEL = DSL.field(DSL.name("u", "auth_level"), String.class);
    private static final Field<Boolean> U_IS_ENABLED = DSL.field(DSL.name("u", "is_enabled"), Boolean.class);
    private static final Field<LocalDateTime> U_CREATED_AT = DSL.field(DSL.name("u", "created_at"),
            LocalDateTime.class);
    private static final Field<LocalDateTime> U_UPDATED_AT = DSL.field(DSL.name("u", "updated_at"),
            LocalDateTime.class);
    private static final Field<LocalDateTime> U_LAST_LOGIN = DSL.field(DSL.name("u", "last_login"),
            LocalDateTime.class);

    private static final Field<Long> UR_USER_ID = DSL.field(DSL.name("ur", "user_id"), Long.class);
    private static final Field<Long> UR_ROLE_ID = DSL.field(DSL.name("ur", "role_id"), Long.class);

    private static final Field<Long> R_ID = DSL.field(DSL.name("r", "id"), Long.class);
    private static final Field<String> R_NAME = DSL.field(DSL.name("r", "name"), String.class);
    private static final Field<String> R_DESCRIPTION = DSL.field(DSL.name("r", "description"), String.class);
    private static final Field<LocalDateTime> R_CREATED_AT = DSL.field(DSL.name("r", "created_at"),
            LocalDateTime.class);
    private static final Field<LocalDateTime> R_UPDATED_AT = DSL.field(DSL.name("r", "updated_at"),
            LocalDateTime.class);

    private static final Field<Long> RP_ROLE_ID = DSL.field(DSL.name("rp", "role_id"), Long.class);
    private static final Field<Long> RP_POLICY_ID = DSL.field(DSL.name("rp", "policy_id"), Long.class);
    private static final Field<Boolean> RP_IS_ACTIVE = DSL.field(DSL.name("rp", "is_active"), Boolean.class);

    private static final Field<Long> P_ID = DSL.field(DSL.name("p", "id"), Long.class);
    private static final Field<String> P_NAME = DSL.field(DSL.name("p", "name"), String.class);
    private static final Field<String> P_DESCRIPTION = DSL.field(DSL.name("p", "description"), String.class);
    private static final Field<String> P_TYPE = DSL.field(DSL.name("p", "type"), String.class);
    private static final Field<Boolean> P_IS_ACTIVE = DSL.field(DSL.name("p", "is_active"), Boolean.class);

    private static final Field<Long> UTA_USER_ID = DSL.field(DSL.name("uta", "user_id"), Long.class);
    private static final Field<Long> UTA_BOARD_ID = DSL.field(DSL.name("uta", "board_id"), Long.class);
    private static final Field<Long> UTA_EMPLOYER_ID = DSL.field(DSL.name("uta", "employer_id"), Long.class);
    private static final Field<Long> UTA_TOLI_ID = DSL.field(DSL.name("uta", "toli_id"), Long.class);

    private static final Field<Long> MULTI_ROLE_USER_ID = DSL.field(DSL.name("multi_role", "user_id"), Long.class);

    private final DSLContext dsl;

    public UserQueryDao(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<User> findAll() {
        var u = USERS.as("u");
        return dsl.select(U_ID, U_USERNAME, U_EMAIL, U_FULL_NAME, U_ROLE, U_IS_ENABLED,
                U_CREATED_AT, U_UPDATED_AT, U_LAST_LOGIN)
                .from(u)
                .orderBy(U_USERNAME.asc())
                .fetch(this::mapUser);
    }

    public Optional<User> findById(Long id) {
        var u = USERS.as("u");
        return dsl.select(U_ID, U_USERNAME, U_EMAIL, U_FULL_NAME, U_ROLE, U_IS_ENABLED,
                U_CREATED_AT, U_UPDATED_AT, U_LAST_LOGIN)
                .from(u)
                .where(U_ID.eq(id))
                .fetchOptional(this::mapUser);
    }

    public Optional<User> findByUsername(String username) {
        var u = USERS.as("u");
        return dsl.select(U_ID, U_USERNAME, U_EMAIL, U_FULL_NAME, U_ROLE, U_IS_ENABLED,
                U_CREATED_AT, U_UPDATED_AT, U_LAST_LOGIN)
                .from(u)
                .where(U_USERNAME.eq(username))
                .fetchOptional(this::mapUser);
    }

    public Optional<User> findByEmail(String email) {
        var u = USERS.as("u");
        return dsl.select(U_ID, U_USERNAME, U_EMAIL, U_FULL_NAME, U_ROLE, U_IS_ENABLED,
                U_CREATED_AT, U_UPDATED_AT, U_LAST_LOGIN)
                .from(u)
                .where(U_EMAIL.eq(email))
                .fetchOptional(this::mapUser);
    }

    public List<User> findByRole(UserRole role) {
        var u = USERS.as("u");
        return dsl.select(U_ID, U_USERNAME, U_EMAIL, U_FULL_NAME, U_ROLE, U_IS_ENABLED,
                U_CREATED_AT, U_UPDATED_AT, U_LAST_LOGIN)
                .from(u)
                .where(U_ROLE.eq(role.name()))
                .orderBy(U_USERNAME.asc())
                .fetch(this::mapUser);
    }

    public List<User> findActiveUsers() {
        var u = USERS.as("u");
        return dsl.select(U_ID, U_USERNAME, U_EMAIL, U_FULL_NAME, U_ROLE, U_IS_ENABLED,
                U_CREATED_AT, U_UPDATED_AT, U_LAST_LOGIN)
                .from(u)
                .where(U_IS_ENABLED.isTrue())
                .orderBy(U_USERNAME.asc())
                .fetch(this::mapUser);
    }

    public List<User> findInactiveUsers() {
        var u = USERS.as("u");
        return dsl.select(U_ID, U_USERNAME, U_EMAIL, U_FULL_NAME, U_ROLE, U_IS_ENABLED,
                U_CREATED_AT, U_UPDATED_AT, U_LAST_LOGIN)
                .from(u)
                .where(U_IS_ENABLED.isFalse())
                .orderBy(U_USERNAME.asc())
                .fetch(this::mapUser);
    }

    public boolean existsByUsername(String username) {
        var u = USERS.as("u");
        return dsl.fetchExists(u, U_USERNAME.eq(username));
    }

    public boolean existsByEmail(String email) {
        var u = USERS.as("u");
        return dsl.fetchExists(u, U_EMAIL.eq(email));
    }

    public List<User> findByRoleId(Long roleId) {
        var u = USERS.as("u");
        var ur = USER_ROLES.as("ur");
        return dsl.select(U_ID, U_USERNAME, U_EMAIL, U_FULL_NAME, U_ROLE, U_IS_ENABLED,
                U_CREATED_AT, U_UPDATED_AT, U_LAST_LOGIN)
                .from(u)
                .join(ur).on(U_ID.eq(UR_USER_ID))
                .where(UR_ROLE_ID.eq(roleId))
                .orderBy(U_USERNAME.asc())
                .fetch(this::mapUser);
    }

    public List<User> findByRoleName(String roleName) {
        var u = USERS.as("u");
        var ur = USER_ROLES.as("ur");
        var r = ROLES.as("r");
        Condition roleMatch = DSL.lower(R_NAME).eq(roleName.toLowerCase());
        return dsl.selectDistinct(U_ID, U_USERNAME, U_EMAIL, U_FULL_NAME, U_ROLE, U_IS_ENABLED,
                U_CREATED_AT, U_UPDATED_AT, U_LAST_LOGIN)
                .from(u)
                .join(ur).on(U_ID.eq(UR_USER_ID))
                .join(r).on(UR_ROLE_ID.eq(R_ID))
                .where(roleMatch)
                .orderBy(U_USERNAME.asc())
                .fetch(this::mapUser);
    }

    public List<User> findUsersWithMultipleRoles() {
        var u = USERS.as("u");
        var ur = USER_ROLES.as("ur");
        var multiRole = DSL.select(UR_USER_ID)
                .from(ur)
                .groupBy(UR_USER_ID)
                .having(DSL.count().gt(1))
                .asTable("multi_role");

        return dsl.select(U_ID, U_USERNAME, U_EMAIL, U_FULL_NAME, U_ROLE, U_IS_ENABLED,
                U_CREATED_AT, U_UPDATED_AT, U_LAST_LOGIN)
                .from(u)
                .join(multiRole).on(U_ID.eq(MULTI_ROLE_USER_ID))
                .orderBy(U_USERNAME.asc())
                .fetch(this::mapUser);
    }

    /**
     * Fetch fully hydrated user views with role and policy metadata using jOOQ.
     * Used for read-heavy endpoints to avoid eager-loading via Hibernate.
     */
    public List<UserWithDetails> findAllWithDetails() {
        return fetchUsersWithDetails(null);
    }

    /**
     * Fetch users filtered by legacy primary role using jOOQ.
     */
    public List<UserWithDetails> findByPrimaryRole(UserRole role) {
        if (role == null) {
            return List.of();
        }
        return fetchUsersWithDetails(U_ROLE.eq(role.name()));
    }

    /**
     * Fetch users filtered by role name (legacy or policy-driven) using jOOQ.
     */
    public List<UserWithDetails> findByRoleNameWithDetails(String roleName) {
        if (roleName == null) {
            return List.of();
        }
        String normalized = roleName.trim().toLowerCase();
        if (normalized.isEmpty()) {
            return List.of();
        }
        Condition roleMatch = DSL.lower(R_NAME).eq(normalized)
                .or(DSL.lower(U_ROLE).eq(normalized));
        return fetchUsersWithDetails(roleMatch);
    }

    private List<UserWithDetails> fetchUsersWithDetails(Condition condition) {
        var u = USERS.as("u");
        var ur = USER_ROLES.as("ur");
        var r = ROLES.as("r");
        var rp = ROLE_POLICIES.as("rp");
        var p = POLICIES.as("p");
        var uta = USER_TENANT_ACL.as("uta");

        Condition effectiveCondition = DSL.trueCondition();
        if (condition != null) {
            effectiveCondition = effectiveCondition.and(condition);
        }

        var records = dsl.select(
                U_ID,
                U_USERNAME,
                U_EMAIL,
                U_FULL_NAME,
                U_ROLE,
                U_USER_TYPE,
                U_AUTH_LEVEL,
                U_IS_ENABLED,
                U_CREATED_AT,
                U_UPDATED_AT,
                U_LAST_LOGIN,
                R_ID,
                R_NAME,
                R_DESCRIPTION,
                P_ID,
                P_NAME,
                P_DESCRIPTION,
                P_TYPE,
                UTA_BOARD_ID,
                UTA_EMPLOYER_ID,
                UTA_TOLI_ID)
                .from(u)
                .leftJoin(ur).on(U_ID.eq(UR_USER_ID))
                .leftJoin(r).on(UR_ROLE_ID.eq(R_ID))
                .leftJoin(rp).on(R_ID.eq(RP_ROLE_ID).and(RP_IS_ACTIVE.isTrue()))
                .leftJoin(p).on(RP_POLICY_ID.eq(P_ID).and(P_IS_ACTIVE.isTrue()))
                .leftJoin(uta).on(U_ID.eq(UTA_USER_ID))
                .where(effectiveCondition)
                .orderBy(U_USERNAME.asc(), R_NAME.asc().nullsLast(), P_NAME.asc().nullsLast())
                .fetch();

        Map<Long, UserWithDetails> users = new LinkedHashMap<>();
        Map<Long, Map<Long, RoleWithPolicies>> rolesByUser = new LinkedHashMap<>();

        records.forEach(record -> {
            Long userId = record.get(U_ID);
            if (userId == null) {
                return;
            }

            UserWithDetails user = users.computeIfAbsent(userId, id -> {
                UserWithDetails dto = new UserWithDetails();
                dto.setId(id);
                dto.setUsername(record.get(U_USERNAME));
                dto.setEmail(record.get(U_EMAIL));
                dto.setFullName(record.get(U_FULL_NAME));
                dto.setUserType(record.get(U_USER_TYPE));
                dto.setAuthLevel(record.get(U_AUTH_LEVEL));
                dto.setEnabled(record.get(U_IS_ENABLED));
                dto.setCreatedAt(record.get(U_CREATED_AT));
                dto.setUpdatedAt(record.get(U_UPDATED_AT));
                dto.setLastLogin(record.get(U_LAST_LOGIN));

                String legacyRole = record.get(U_ROLE);
                if (legacyRole != null) {
                    try {
                        dto.setPrimaryRole(UserRole.valueOf(legacyRole));
                    } catch (IllegalArgumentException ignored) {
                        dto.setPrimaryRole(null);
                    }
                }
                return dto;
            });

            if (user.getBoardId() == null) {
                Long boardId = record.get(UTA_BOARD_ID);
                if (boardId != null) {
                    user.setBoardId(boardId);
                }
            }

            if (user.getEmployerId() == null) {
                Long employerId = record.get(UTA_EMPLOYER_ID);
                if (employerId != null) {
                    user.setEmployerId(employerId);
                }
            }

            if (user.getToliId() == null) {
                Long toliId = record.get(UTA_TOLI_ID);
                if (toliId != null) {
                    user.setToliId(toliId);
                }
            }

            Long roleId = record.get(R_ID);
            if (roleId == null) {
                return;
            }

            Map<Long, RoleWithPolicies> userRoles = rolesByUser.computeIfAbsent(userId,
                    ignore -> new LinkedHashMap<>());
            RoleWithPolicies role = userRoles.computeIfAbsent(roleId, rid -> {
                RoleWithPolicies dto = new RoleWithPolicies();
                dto.setId(rid);
                dto.setName(record.get(R_NAME));
                dto.setDescription(record.get(R_DESCRIPTION));
                user.getRoles().add(dto);
                return dto;
            });

            Long policyId = record.get(P_ID);
            if (policyId == null) {
                return;
            }

            PolicySummary policy = new PolicySummary();
            policy.setId(policyId);
            policy.setName(record.get(P_NAME));
            policy.setDescription(record.get(P_DESCRIPTION));
            policy.setType(record.get(P_TYPE));
            role.getPolicies().add(policy);
        });

        return new ArrayList<>(users.values());
    }

    public int countUsersByRole(UserRole role) {
        var u = USERS.as("u");
        return dsl.fetchCount(u, U_ROLE.eq(role.name()));
    }

    public long countActiveUsers() {
        var u = USERS.as("u");
        return (long) dsl.fetchCount(u, U_IS_ENABLED.isTrue());
    }

    public List<User> searchUsers(String searchTerm) {
        var u = USERS.as("u");
        String effectiveTerm = searchTerm == null ? "" : searchTerm.toLowerCase();
        String pattern = "%" + effectiveTerm + "%";
        Condition usernameMatch = DSL.lower(U_USERNAME).like(pattern);
        Condition emailMatch = DSL.lower(U_EMAIL).like(pattern);
        Condition fullNameMatch = DSL.lower(U_FULL_NAME).like(pattern);

        return dsl.select(U_ID, U_USERNAME, U_EMAIL, U_FULL_NAME, U_ROLE, U_IS_ENABLED,
                U_CREATED_AT, U_UPDATED_AT, U_LAST_LOGIN)
                .from(u)
                .where(usernameMatch.or(emailMatch).or(fullNameMatch))
                .orderBy(U_USERNAME.asc())
                .fetch(this::mapUser);
    }

    public static class UserWithDetails {
        private Long id;
        private String username;
        private String email;
        private String fullName;
        private String userType;
        private String authLevel;
        private UserRole primaryRole;
        private Boolean enabled;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime lastLogin;
        private Long boardId;
        private Long employerId;
        private Long toliId;

        private final Set<RoleWithPolicies> roles = new LinkedHashSet<>();

        public String getUserType() {
            return userType;
        }

        public void setUserType(String userType) {
            this.userType = userType;
        }

        public String getAuthLevel() {
            return authLevel;
        }

        public void setAuthLevel(String authLevel) {
            this.authLevel = authLevel;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public UserRole getPrimaryRole() {
            return primaryRole;
        }

        public void setPrimaryRole(UserRole primaryRole) {
            this.primaryRole = primaryRole;
        }

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public LocalDateTime getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
        }

        public LocalDateTime getLastLogin() {
            return lastLogin;
        }

        public void setLastLogin(LocalDateTime lastLogin) {
            this.lastLogin = lastLogin;
        }

        public Long getBoardId() {
            return boardId;
        }

        public void setBoardId(Long boardId) {
            this.boardId = boardId;
        }

        public Long getEmployerId() {
            return employerId;
        }

        public void setEmployerId(Long employerId) {
            this.employerId = employerId;
        }

        public Long getToliId() {
            return toliId;
        }

        public void setToliId(Long toliId) {
            this.toliId = toliId;
        }

        public Set<RoleWithPolicies> getRoles() {
            return roles;
        }
    }

    public static class RoleWithPolicies {
        private Long id;
        private String name;
        private String description;
        private final Set<PolicySummary> policies = new LinkedHashSet<>();

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Set<PolicySummary> getPolicies() {
            return policies;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof RoleWithPolicies))
                return false;
            RoleWithPolicies that = (RoleWithPolicies) o;
            return Objects.equals(id, that.id);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(id);
        }
    }

    public static class PolicySummary {
        private Long id;
        private String name;
        private String description;
        private String type;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof PolicySummary))
                return false;
            PolicySummary that = (PolicySummary) o;
            return Objects.equals(id, that.id);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(id);
        }
    }

    private User mapUser(Record record) {
        User user = new User();
        Long id = record.get(U_ID);
        if (id != null) {
            user.setId(id);
        }
        user.setUsername(record.get(U_USERNAME));
        user.setEmail(record.get(U_EMAIL));
        user.setFullName(record.get(U_FULL_NAME));

        String roleValue = record.get(U_ROLE);
        if (roleValue != null) {
            user.setRole(UserRole.valueOf(roleValue));
        }

        Boolean enabled = record.get(U_IS_ENABLED);
        if (enabled != null) {
            user.setEnabled(enabled);
        }

        LocalDateTime createdAt = record.get(U_CREATED_AT);
        if (createdAt != null) {
            user.setCreatedAt(createdAt);
        }

        LocalDateTime updatedAt = record.get(U_UPDATED_AT);
        if (updatedAt != null) {
            user.setUpdatedAt(updatedAt);
        }

        LocalDateTime lastLogin = record.get(U_LAST_LOGIN);
        if (lastLogin != null) {
            user.setLastLogin(lastLogin);
        }

        return user;
    }
}
