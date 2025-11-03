# User Management CRUD Completion

## Overview
This document describes the addition of missing Edit User and Delete User endpoints to complete the user management CRUD operations in the auth service.

## Problem Statement
During the bootstrap setup verification, we discovered that two page actions in the User Management page did not have corresponding API endpoints:
- **Edit User** (action_id=4): Capability `user.account.update` had no endpoint
- **Delete User** (action_id=5): Capability `user.account.delete` had no endpoint

This meant the UI had buttons for editing and deleting users, but there were no backend API endpoints to handle these actions.

## Solution

### 1. Created UpdateUserRequest DTO
**File**: `src/main/java/com/example/userauth/dto/UpdateUserRequest.java`

A new DTO to handle user update requests with the following fields:
- `username`: Optional, validated for length (3-50 characters)
- `email`: Optional, validated for email format
- `password`: Optional, validated for minimum length (8 characters)
- `fullName`: Optional

### 2. Added Service Methods
**File**: `src/main/java/com/example/userauth/service/AuthService.java`

Added two new methods to `AuthService`:

#### `updateUser(Long userId, UpdateUserRequest request)`
- Updates user information based on provided fields
- Validates uniqueness of username and email before updating
- Hashes password if provided
- Increments `permissionVersion` when password changes (to invalidate JWT tokens)
- Returns the updated `User` entity

#### `deleteUser(Long userId)`
- Performs a **soft delete** by setting `enabled = false`
- Increments `permissionVersion` to invalidate all existing JWT tokens
- Does not remove the user record from the database (can be recovered)

### 3. Added Controller Endpoints
**File**: `src/main/java/com/example/userauth/controller/AuthController.java`

Added two new RESTful endpoints:

#### PUT `/api/auth/users/{userId}`
- Updates user information
- Requires authentication
- Auditable action: `UPDATE_USER`
- Returns updated user details or error message

#### DELETE `/api/auth/users/{userId}`
- Soft deletes a user account
- Requires authentication
- Auditable action: `DELETE_USER`
- Returns success message or error

### 4. Database Registration
**Script**: `docs/onboarding/setup/11_add_missing_user_endpoints.sql`

Registered the new endpoints in the database:
- **Endpoint ID 71**: PUT `/api/auth/users/{userId}` - "Update user information"
- **Endpoint ID 72**: DELETE `/api/auth/users/{userId}` - "Delete user (soft delete)"

### 5. Authorization Setup

#### Linked Endpoints to Policy
Both endpoints were linked to `USER_ACCOUNT_MANAGE_POLICY` (policy_id=2):
- This policy grants access to BUSINESS_ADMIN and TECHNICAL_BOOTSTRAP roles
- Ensures only authorized users can update or delete user accounts

#### Updated Page Actions
- **Edit User** (action_id=4): Linked to endpoint_id=71 (PUT)
- **Delete User** (action_id=5): Linked to endpoint_id=72 (DELETE)

## Verification

### Complete User Management CRUD Operations
| Action | Method | Path | Endpoint ID | Capability | Policy |
|--------|--------|------|-------------|------------|--------|
| Create | POST | `/api/auth/users` | 3 | `user.account.create` | USER_ACCOUNT_MANAGE_POLICY |
| Read | GET | `/api/auth/users` | 5 | `user.account.read` | USER_ACCOUNT_MANAGE_POLICY |
| Update | PUT | `/api/auth/users/{userId}` | **71** | `user.account.update` | USER_ACCOUNT_MANAGE_POLICY |
| Delete | DELETE | `/api/auth/users/{userId}` | **72** | `user.account.delete` | USER_ACCOUNT_MANAGE_POLICY |
| Toggle Status | PUT | `/api/auth/users/{userId}/status` | 7 | `user.account.toggle` | USER_ACCOUNT_MANAGE_POLICY |

### Authorization Chain
```
User → Role → Policy → Capability ↔ Endpoint (authorization)
                                 ↓
                        PageAction → Endpoint (UI binding)
```

For Edit User and Delete User:
1. **Backend Authorization**: 
   - Capability (`user.account.update`/`user.account.delete`) 
   - ↔ Endpoint (via `USER_ACCOUNT_MANAGE_POLICY`)
2. **Frontend UI Binding**: 
   - PageAction → Endpoint (direct FK relationship)

### API Response Example

#### Update User
```json
PUT /api/auth/users/1
{
  "username": "new.username",
  "email": "new.email@example.com",
  "fullName": "New Full Name"
}

Response:
{
  "message": "User updated successfully",
  "userId": 1,
  "username": "new.username",
  "email": "new.email@example.com",
  "fullName": "New Full Name"
}
```

#### Delete User
```json
DELETE /api/auth/users/1

Response:
{
  "message": "User deleted successfully",
  "userId": 1
}
```

## Security Features

### 1. Soft Delete
- Users are disabled (`enabled = false`) instead of being permanently deleted
- Preserves audit trail and historical data
- Allows recovery if deletion was accidental

### 2. Token Invalidation
- Both operations increment the user's `permissionVersion`
- Forces the user to re-authenticate on next request
- Prevents the use of old JWT tokens

### 3. Validation
- Username uniqueness checked before update
- Email uniqueness checked before update
- Input validation via Jakarta Bean Validation annotations

### 4. Audit Trail
- Both operations are marked with `@Auditable` annotations
- Automatically logged via the shared entity audit system

## Testing

### Manual Testing with cURL

#### Update User
```bash
curl -X PUT http://localhost:8080/api/auth/users/1 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "updated.user",
    "email": "updated@example.com",
    "fullName": "Updated User Name"
  }'
```

#### Delete User
```bash
curl -X DELETE http://localhost:8080/api/auth/users/1 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Database Verification
```sql
-- Check if endpoints are registered
SELECT id, method, path, description 
FROM auth.endpoints 
WHERE id IN (71, 72);

-- Check endpoint-policy links
SELECT e.id, e.method, e.path, p.name as policy
FROM auth.endpoints e
JOIN auth.endpoint_policies ep ON e.id = ep.endpoint_id
JOIN auth.policies p ON ep.policy_id = p.id
WHERE e.id IN (71, 72);

-- Check page action links
SELECT pa.id, pa.label, pa.endpoint_id, e.method, e.path
FROM auth.page_actions pa
JOIN auth.endpoints e ON pa.endpoint_id = e.id
WHERE pa.id IN (4, 5);
```

## Related Files

### Java Files
- `src/main/java/com/example/userauth/dto/UpdateUserRequest.java`
- `src/main/java/com/example/userauth/service/AuthService.java`
- `src/main/java/com/example/userauth/controller/AuthController.java`

### SQL Files
- `docs/onboarding/setup/11_add_missing_user_endpoints.sql`

### Documentation
- `docs/onboarding/setup/README.md` (should be updated to reference script 11)

## Future Enhancements

1. **Hard Delete Option**: Add an optional query parameter to perform permanent deletion
2. **Bulk Operations**: Support updating or deleting multiple users at once
3. **Patch Support**: Add PATCH endpoint for partial updates
4. **Password Reset**: Separate endpoint specifically for password changes with confirmation
5. **Recovery**: Add endpoint to restore soft-deleted users

## Conclusion
The user management CRUD functionality is now complete. All page actions in the User Management UI have corresponding backend endpoints with proper authorization checks. The system maintains a complete audit trail and uses soft deletes to preserve data integrity.
