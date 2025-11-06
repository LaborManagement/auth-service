package com.example.userauth.dao;

import com.example.userauth.entity.Role;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Read-only role access backed by jOOQ DSL for stronger compile-time guarantees.
 */
@Repository
public class RoleQueryDao {

    private static final Table<Record> ROLES = DSL.table(DSL.name("roles"));
    private static final Table<Record> ROLE_PERMISSIONS = DSL.table(DSL.name("role_permissions"));
    private static final Table<Record> USER_ROLES = DSL.table(DSL.name("user_roles"));
    private static final Table<Record> USERS = DSL.table(DSL.name("users"));

    private static final Field<Long> R_ID = DSL.field(DSL.name("r", "id"), Long.class);
    private static final Field<String> R_NAME = DSL.field(DSL.name("r", "name"), String.class);
    private static final Field<String> R_DESCRIPTION = DSL.field(DSL.name("r", "description"), String.class);
    private static final Field<LocalDateTime> R_CREATED_AT = DSL.field(DSL.name("r", "created_at"), LocalDateTime.class);
    private static final Field<LocalDateTime> R_UPDATED_AT = DSL.field(DSL.name("r", "updated_at"), LocalDateTime.class);

    private static final Field<Long> RP_ROLE_ID = DSL.field(DSL.name("rp", "role_id"), Long.class);
    private static final Field<Long> RP_PERMISSION_ID = DSL.field(DSL.name("rp", "permission_id"), Long.class);

    private static final Field<Long> UR_ROLE_ID = DSL.field(DSL.name("ur", "role_id"), Long.class);
    private static final Field<Long> UR_USER_ID = DSL.field(DSL.name("ur", "user_id"), Long.class);

    private static final Field<String> U_USERNAME = DSL.field(DSL.name("u", "username"), String.class);
    private static final Field<Long> U_ID = DSL.field(DSL.name("u", "id"), Long.class);

    private static final Field<Integer> PERMISSION_COUNT = DSL.field(DSL.name("permission_count"), Integer.class);
    private static final Field<Integer> USER_COUNT = DSL.field(DSL.name("user_count"), Integer.class);

    private final DSLContext dsl;

    public RoleQueryDao(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<Role> findAll() {
        var r = ROLES.as("r");
        return dsl.select(R_ID, R_NAME, R_DESCRIPTION, R_CREATED_AT, R_UPDATED_AT)
                .from(r)
                .orderBy(R_NAME.asc())
                .fetch(this::mapRole);
    }

    public Optional<Role> findById(Long id) {
        var r = ROLES.as("r");
        return dsl.select(R_ID, R_NAME, R_DESCRIPTION, R_CREATED_AT, R_UPDATED_AT)
                .from(r)
                .where(R_ID.eq(id))
                .fetchOptional(this::mapRole);
    }

    public Optional<Role> findByName(String name) {
        var r = ROLES.as("r");
        return dsl.select(R_ID, R_NAME, R_DESCRIPTION, R_CREATED_AT, R_UPDATED_AT)
                .from(r)
                .where(R_NAME.eq(name))
                .fetchOptional(this::mapRole);
    }

    public List<Role> findByUsername(String username) {
        var r = ROLES.as("r");
        var ur = USER_ROLES.as("ur");
        var u = USERS.as("u");
        return dsl.select(R_ID, R_NAME, R_DESCRIPTION, R_CREATED_AT, R_UPDATED_AT)
                .from(r)
                .join(ur).on(R_ID.eq(UR_ROLE_ID))
                .join(u).on(UR_USER_ID.eq(U_ID))
                .where(U_USERNAME.eq(username))
                .orderBy(R_NAME.asc())
                .fetch(this::mapRole);
    }

    public List<Role> findByUserId(Long userId) {
        var r = ROLES.as("r");
        var ur = USER_ROLES.as("ur");
        return dsl.select(R_ID, R_NAME, R_DESCRIPTION, R_CREATED_AT, R_UPDATED_AT)
                .from(r)
                .join(ur).on(R_ID.eq(UR_ROLE_ID))
                .where(UR_USER_ID.eq(userId))
                .orderBy(R_NAME.asc())
                .fetch(this::mapRole);
    }

    public boolean existsByName(String name) {
        var r = ROLES.as("r");
        return dsl.fetchExists(r, R_NAME.eq(name));
    }

    public int countUsersWithRole(Long roleId) {
        var ur = USER_ROLES.as("ur");
        return dsl.fetchCount(ur, UR_ROLE_ID.eq(roleId));
    }

    public int countPermissionsInRole(Long roleId) {
        var rp = ROLE_PERMISSIONS.as("rp");
        return dsl.fetchCount(rp, RP_ROLE_ID.eq(roleId));
    }

    public List<RoleWithPermissionCount> findAllWithPermissionCounts() {
        var r = ROLES.as("r");
        var rp = ROLE_PERMISSIONS.as("rp");
        var ur = USER_ROLES.as("ur");

        return dsl.select(
                        R_ID,
                        R_NAME,
                        R_DESCRIPTION,
                        R_CREATED_AT,
                        R_UPDATED_AT,
                        DSL.countDistinct(RP_PERMISSION_ID).as("permission_count"),
                        DSL.countDistinct(UR_USER_ID).as("user_count")
                )
                .from(r)
                .leftJoin(rp).on(R_ID.eq(RP_ROLE_ID))
                .leftJoin(ur).on(R_ID.eq(UR_ROLE_ID))
                .groupBy(R_ID, R_NAME, R_DESCRIPTION, R_CREATED_AT, R_UPDATED_AT)
                .orderBy(R_NAME.asc())
                .fetch(record -> {
                    RoleWithPermissionCount dto = new RoleWithPermissionCount();
                    dto.setId(record.get(R_ID));
                    dto.setName(record.get(R_NAME));
                    dto.setDescription(record.get(R_DESCRIPTION));
                    dto.setPermissionCount(Optional.ofNullable(record.get(PERMISSION_COUNT)).orElse(0));
                    dto.setUserCount(Optional.ofNullable(record.get(USER_COUNT)).orElse(0));
                    return dto;
                });
    }

    private Role mapRole(Record record) {
        Role role = new Role();
        Long id = record.get(R_ID);
        if (id != null) {
            role.setId(id);
        }
        role.setName(record.get(R_NAME));
        role.setDescription(record.get(R_DESCRIPTION));

        LocalDateTime createdAt = record.get(R_CREATED_AT);
        if (createdAt != null) {
            role.setCreatedAt(createdAt);
        }

        LocalDateTime updatedAt = record.get(R_UPDATED_AT);
        if (updatedAt != null) {
            role.setUpdatedAt(updatedAt);
        }

        return role;
    }

    public static class RoleWithPermissionCount {
        private Long id;
        private String name;
        private String description;
        private int permissionCount;
        private int userCount;

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

        public int getPermissionCount() {
            return permissionCount;
        }

        public void setPermissionCount(int permissionCount) {
            this.permissionCount = permissionCount;
        }

        public int getUserCount() {
            return userCount;
        }

        public void setUserCount(int userCount) {
            this.userCount = userCount;
        }
    }
}
