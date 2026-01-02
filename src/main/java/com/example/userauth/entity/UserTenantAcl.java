/**
 * UserTenantAcl entity mapping to auth.user_tenant_acl table
 * Represents the permission matrix that controls which users can read/write which tenant data.
 */
package com.example.userauth.entity;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "user_tenant_acl", schema = "auth", uniqueConstraints = @UniqueConstraint(columnNames = {
        "user_id", "board_id", "employer_id", "toli_id" }))
public class UserTenantAcl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "board_id", nullable = false)
    private Long boardId;

    @Column(name = "employer_id")
    private Long employerId;

    @Column(name = "can_read", nullable = false)
    private Boolean canRead = true;

    @Column(name = "can_write", nullable = false)
    private Boolean canWrite = false;

    @Column(name = "toli_id")
    private Long toliId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Constructors
    public UserTenantAcl() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UserTenantAcl(Long userId, Long boardId, Long employerId) {
        this();
        this.userId = userId;
        this.boardId = boardId;
        this.employerId = employerId;
    }

    public UserTenantAcl(Long userId, Long boardId, Long employerId, Boolean canRead, Boolean canWrite) {
        this();
        this.userId = userId;
        this.boardId = boardId;
        this.employerId = employerId;
        this.canRead = canRead;
        this.canWrite = canWrite;
    }

    // Getters and Setters
    public Long getToliId() {
        return toliId;
    }

    public void setToliId(Long toliId) {
        this.toliId = toliId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public Boolean getCanRead() {
        return canRead;
    }

    public void setCanRead(Boolean canRead) {
        this.canRead = canRead;
    }

    public Boolean getCanWrite() {
        return canWrite;
    }

    public void setCanWrite(Boolean canWrite) {
        this.canWrite = canWrite;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof UserTenantAcl))
            return false;
        UserTenantAcl that = (UserTenantAcl) o;
        return Objects.equals(userId, that.userId) &&
                Objects.equals(boardId, that.boardId) &&
                Objects.equals(employerId, that.employerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, boardId, employerId);
    }

    @Override
    public String toString() {
        return "UserTenantAcl{" +
                "id=" + id +
                ", userId=" + userId +
                ", boardId='" + boardId + '\'' +
                ", employerId='" + employerId + '\'' +
                ", canRead=" + canRead +
                ", canWrite=" + canWrite +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
