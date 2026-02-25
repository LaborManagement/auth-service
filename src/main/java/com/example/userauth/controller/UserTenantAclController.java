package com.example.userauth.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.userauth.dto.EmployerAccessDto;
import com.example.userauth.dto.UserTenantAclDetailDto;
import com.example.userauth.entity.UserTenantAcl;
import com.example.userauth.repository.UserTenantAclRepository;
import com.example.userauth.service.AuthService;
import com.example.userauth.service.UserTenantAclService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/auth-service/api/user-tenant-acl")
@Tag(name = "User Tenant Access", description = "Manage user to employer/toli access combinations")
@SecurityRequirement(name = "Bearer Authentication")
public class UserTenantAclController {

    @Autowired
    private UserTenantAclRepository aclRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserTenantAclService userTenantAclService;

    public record UpsertRequest(
            @NotNull Long userId,
            @NotNull Long boardId,
            Long employerId,
            Long toliId,
            Boolean canRead,
            Boolean canWrite) {
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get enriched ACL entries for user", description = "Returns user tenant ACL entries with employer and toli details including registration numbers and names", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "ACL entries retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<UserTenantAclDetailDto>> list(@PathVariable Long userId) {
        List<UserTenantAclDetailDto> details = userTenantAclService.getUserTenantAclDetails(userId);
        return ResponseEntity.ok(details);
    }

    @GetMapping("/employers/me")
    @Operation(summary = "Employers accessible to current user", description = "Returns employers where user has ACL and employer/toli relation exists", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<List<EmployerAccessDto>> listEmployersForCurrentUser() {
        Long userId = authService.getCurrentUser()
                .map(u -> u.getId())
                .orElseThrow(() -> new RuntimeException("No authenticated user"));
        return ResponseEntity.ok(userTenantAclService.getEmployersForUser(userId));
    }

    @PostMapping
    @Operation(summary = "Add/replace a single ACL row", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<?> add(@Valid @RequestBody UpsertRequest request) {
        UserTenantAcl acl = new UserTenantAcl();
        acl.setUserId(request.userId());
        acl.setBoardId(request.boardId());
        acl.setEmployerId(request.employerId());
        acl.setToliId(request.toliId());
        boolean canWrite = request.canWrite() != null ? request.canWrite() : false;
        boolean canRead = request.canRead() != null ? request.canRead() : true;
        if (canWrite && !canRead) {
            canRead = true;
        }
        acl.setCanRead(canRead);
        acl.setCanWrite(canWrite);
        aclRepository.save(acl);
        authService.updateUserPermissions(request.userId());
        return ResponseEntity.ok(Map.of("message", "ACL saved"));
    }

    @DeleteMapping
    @Operation(summary = "Delete ACL rows for user + optional employer/toli", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<?> delete(
            @RequestParam Long userId,
            @RequestParam Long boardId,
            @RequestParam(required = false) Long employerId,
            @RequestParam(required = false) Long toliId) {
        List<UserTenantAcl> rows = aclRepository.findByUserId(userId).stream()
                .filter(r -> r.getBoardId().equals(boardId))
                .filter(r -> employerId == null || (r.getEmployerId() != null && r.getEmployerId().equals(employerId)))
                .filter(r -> toliId == null || (r.getToliId() != null && r.getToliId().equals(toliId)))
                .toList();
        aclRepository.deleteAll(rows);
        authService.updateUserPermissions(userId);
        return ResponseEntity.ok(Map.of("deleted", rows.size()));
    }
}
