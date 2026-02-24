package com.example.userauth.controller;

import com.example.userauth.entity.UserTenantAcl;
import com.example.userauth.repository.UserTenantAclRepository;
import com.example.userauth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth-service/api/user-tenant-acl")
@Tag(name = "User Tenant Access", description = "Manage user to employer/toli access combinations")
@SecurityRequirement(name = "Bearer Authentication")
public class UserTenantAclController {

    @Autowired
    private UserTenantAclRepository aclRepository;

    @Autowired
    private AuthService authService;

    public record UpsertRequest(
            @NotNull Long userId,
            @NotNull Long boardId,
            Long employerId,
            Long toliId,
            Boolean canRead,
            Boolean canWrite
    ) {}

    @GetMapping("/{userId}")
    @Operation(summary = "List ACL entries for user", security = @SecurityRequirement(name = "Bearer Authentication"))
    public List<UserTenantAcl> list(@PathVariable Long userId) {
        return aclRepository.findByUserId(userId);
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
            @RequestParam(required = false) Long toliId
    ) {
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
