package com.example.userauth.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.userauth.dao.RoleQueryDao.RoleWithPermissionCount;
import com.example.userauth.entity.Role;
import com.example.userauth.entity.User;
import com.example.userauth.service.RoleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shared.common.annotation.Auditable;
import com.shared.common.util.ETagUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Admin controller for managing roles
 */
@RestController
@RequestMapping("/auth-service/api/admin/roles")
@Tag(name = "Role Management", description = "APIs for managing roles")
@SecurityRequirement(name = "Bearer Authentication")
public class RoleController {

    private static final Logger logger = LoggerFactory.getLogger(RoleController.class);

    @Autowired
    private RoleService roleService;

    @Autowired
    private ObjectMapper objectMapper;

    @Auditable(action = "GET_ALL_ROLES", resourceType = "ROLE")
    @GetMapping
    @Operation(summary = "Get all roles", description = "Returns all roles without policy details.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Roles retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<Map<String, Object>>> getAllRoles(HttpServletRequest request) {
        List<Role> roles = roleService.getAllRoles();
        List<Map<String, Object>> response = roles.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        try {
            String responseJson = objectMapper.writeValueAsString(response);
            String eTag = ETagUtil.generateETag(responseJson);
            String ifNoneMatch = request.getHeader(HttpHeaders.IF_NONE_MATCH);
            if (eTag.equals(ifNoneMatch)) {
                return ResponseEntity.status(304).eTag(eTag).build();
            }
            return ResponseEntity.ok().eTag(eTag).body(response);
        } catch (Exception e) {
            logger.error("Error processing roles response", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/with-permissions")
    @Operation(summary = "Get all roles with permissions")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Roles with permissions retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<RoleWithPermissionCount>> getAllRolesWithPermissions(HttpServletRequest request) {
        List<RoleWithPermissionCount> roles = roleService.getAllRolesWithPermissionCounts();
        try {
            String responseJson = objectMapper.writeValueAsString(roles);
            String eTag = ETagUtil.generateETag(responseJson);
            String ifNoneMatch = request.getHeader(HttpHeaders.IF_NONE_MATCH);
            if (eTag.equals(ifNoneMatch)) {
                return ResponseEntity.status(304).eTag(eTag).build();
            }
            return ResponseEntity.ok().eTag(eTag).body(roles);
        } catch (Exception e) {
            logger.error("Error processing roles with permissions response", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get role by ID")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Role found and returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Role not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> getRoleById(@PathVariable Long id, HttpServletRequest request) {
        return (ResponseEntity<Map<String, Object>>) roleService.getRoleById(id)
                .map(role -> {
                    try {
                        Map<String, Object> response = convertToResponse(role);
                        String responseJson = objectMapper.writeValueAsString(response);
                        String eTag = ETagUtil.generateETag(responseJson);
                        String ifNoneMatch = request.getHeader(HttpHeaders.IF_NONE_MATCH);
                        if (eTag.equals(ifNoneMatch)) {
                            return ResponseEntity.status(304).eTag(eTag).build();
                        }
                        return ResponseEntity.ok().eTag(eTag).body(response);
                    } catch (Exception e) {
                        logger.error("Error processing role response", e);
                        return ResponseEntity.internalServerError().build();
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-name/{name}")
    @Operation(summary = "Get role by name")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Role found and returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Role not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> getRoleByName(@PathVariable String name, HttpServletRequest request) {
        return (ResponseEntity<Map<String, Object>>) roleService.getRoleByNameWithPermissions(name)
                .map(role -> {
                    try {
                        Map<String, Object> response = convertToResponse(role);
                        String responseJson = objectMapper.writeValueAsString(response);
                        String eTag = ETagUtil.generateETag(responseJson);
                        String ifNoneMatch = request.getHeader(HttpHeaders.IF_NONE_MATCH);
                        if (eTag.equals(ifNoneMatch)) {
                            return ResponseEntity.status(304).eTag(eTag).build();
                        }
                        return ResponseEntity.ok().eTag(eTag).body(response);
                    } catch (Exception e) {
                        return ResponseEntity.internalServerError().build();
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create new role")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Role created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<Role> createRole(@RequestBody CreateRoleRequest request) {
        try {
            Role role = roleService.createRole(request.getName(), request.getDescription());
            return ResponseEntity.ok(role);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update role")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Role updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<Role> updateRole(@PathVariable Long id,
            @RequestBody UpdateRoleRequest request) {
        try {
            Role role = roleService.updateRole(id, request.getName(), request.getDescription());
            return ResponseEntity.ok(role);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete role")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Role deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Role not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Cannot delete role")
    })
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        try {
            roleService.deleteRole(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * DEPRECATED: Legacy permission endpoints kept for reference only.
     * The active authorization flow relies on Role → Policy → Endpoint mappings.
     */

    // @PostMapping("/{roleId}/permissions/{permissionId}")
    // @Operation(summary = "Add permission to role")
    // public ResponseEntity<Role> addPermissionToRole(@PathVariable Long roleId,
    // @PathVariable Long permissionId) {
    // // OLD SYSTEM - superseded by policy assignments
    // return ResponseEntity.status(HttpStatus.GONE)
    // .body(null); // 410 Gone
    // }

    // @DeleteMapping("/{roleId}/permissions/{permissionId}")
    // @Operation(summary = "Remove permission from role")
    // public ResponseEntity<Role> removePermissionFromRole(@PathVariable Long
    // roleId,
    // @PathVariable Long permissionId) {
    // // OLD SYSTEM - superseded by policy assignments
    // return ResponseEntity.status(HttpStatus.GONE)
    // .body(null); // 410 Gone
    // }

    @PostMapping("/assign")
    @Operation(summary = "Assign role to user")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Role assigned to user successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<User> assignRoleToUser(@RequestBody AssignRoleRequest request) {
        try {
            User user = roleService.assignRoleToUser(request.getUserId(), request.getRoleId());
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/revoke")
    @Operation(summary = "Revoke role from user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role revoked from user successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    public ResponseEntity<User> revokeRoleFromUser(@RequestBody RevokeRoleRequest request) {
        try {
            User user = roleService.revokeRoleFromUser(request.getUserId(), request.getRoleId());
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Request DTOs
    private Map<String, Object> convertToResponse(Role role) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", role.getId());
        response.put("name", role.getName());
        response.put("description", role.getDescription());
        response.put("isActive", role.getIsActive());
        response.put("createdAt", role.getCreatedAt());
        response.put("updatedAt", role.getUpdatedAt());
        return response;
    }

    public static class CreateRoleRequest {
        private String name;
        private String description;

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
    }

    public static class UpdateRoleRequest {
        private String name;
        private String description;

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
    }

    public static class AssignRoleRequest {
        private Long userId;
        private Long roleId;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public Long getRoleId() {
            return roleId;
        }

        public void setRoleId(Long roleId) {
            this.roleId = roleId;
        }
    }

    public static class RevokeRoleRequest {
        private Long userId;
        private Long roleId;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public Long getRoleId() {
            return roleId;
        }

        public void setRoleId(Long roleId) {
            this.roleId = roleId;
        }
    }
}
