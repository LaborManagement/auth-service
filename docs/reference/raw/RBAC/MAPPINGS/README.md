# Endpoint & Policy Mappings

> Complete mappings of endpoints to policies and policies to capabilities for the RBAC system.

---

## 📋 Contents

### 1. Endpoints Extraction
**File**: [PHASE1_ENDPOINTS_EXTRACTION.md](PHASE1_ENDPOINTS_EXTRACTION.md)

**Overview**:
- 100+ endpoints extracted from 3 microservices
- Organized by service and resource type
- HTTP method and path documented
- Purpose and authentication level specified

**Services Covered**:
1. **Auth Service** (25+ endpoints)
   - User authentication & management
   - Role & policy management
   - Token operations

2. **Payment Service** (35+ endpoints)
   - Payment CRUD operations
   - Payment verification
   - Payment reports

3. **Employer Service** (40+ endpoints)
   - Employer management
   - Worker management
   - Configuration

---

### 2. Policy-Capability Mappings
**File**: [PHASE4_POLICY_CAPABILITY_MAPPINGS.md](PHASE4_POLICY_CAPABILITY_MAPPINGS.md)

**Overview**:
- 7 role-based policies defined
- 288 total policy-capability links
- Fine-grained capability assignment per role
- Hierarchical permission structure

**Policies**:
1. **PLATFORM_BOOTSTRAP** (55 capabilities) - System bootstrap only
2. **ADMIN_TECH** (51 capabilities) - Technical administration
3. **ADMIN_OPS** (42 capabilities) - Operations administration
4. **BOARD** (17 capabilities) - Board-level management
5. **EMPLOYER** (19 capabilities) - Employer-level management
6. **WORKER** (14 capabilities) - User-level access
7. **TEST_USER** (50 capabilities) - QA testing

---

### 3. Endpoint-Policy Mappings
**File**: [PHASE5_ENDPOINT_POLICY_MAPPINGS.md](PHASE5_ENDPOINT_POLICY_MAPPINGS.md)

**Overview**:
- 100+ endpoints mapped to roles
- Access matrix showing who can call what endpoint
- HTTP methods and required capabilities specified
- Complete authorization decision table

**Matrix Shows**:
- Which roles can access which endpoints
- Required HTTP methods
- Required capabilities per endpoint
- Response status codes

---

## 🔍 How to Use

### For API Developers
1. **Find Endpoint Documentation**:
   - Check [PHASE1_ENDPOINTS_EXTRACTION.md](PHASE1_ENDPOINTS_EXTRACTION.md)
   - Find your endpoint path
   - Note the HTTP method and parameters

2. **Determine Required Authorization**:
   - Go to [PHASE5_ENDPOINT_POLICY_MAPPINGS.md](PHASE5_ENDPOINT_POLICY_MAPPINGS.md)
   - Find your endpoint in the matrix
   - Identify required roles/capabilities

3. **Implement Authorization**:
   ```java
   @PreAuthorize("hasRole('EMPLOYER')")
   @GetMapping("/api/payments")
   public List<Payment> getPayments() { ... }
   ```

### For Security Architects
1. **Review Policy Structure**:
   - Study [PHASE4_POLICY_CAPABILITY_MAPPINGS.md](PHASE4_POLICY_CAPABILITY_MAPPINGS.md)
   - Verify role hierarchy and permissions
   - Check least privilege principles

2. **Validate Endpoint Coverage**:
   - Compare [PHASE1_ENDPOINTS_EXTRACTION.md](PHASE1_ENDPOINTS_EXTRACTION.md) with actual API
   - Ensure all endpoints have security policies
   - Identify any unprotected endpoints

3. **Audit Access Control**:
   - Use [PHASE5_ENDPOINT_POLICY_MAPPINGS.md](PHASE5_ENDPOINT_POLICY_MAPPINGS.md)
   - Verify access decisions match business requirements
   - Test edge cases and special roles

### For QA/Testing
1. **Test Endpoint Access**:
   - Reference [PHASE5_ENDPOINT_POLICY_MAPPINGS.md](PHASE5_ENDPOINT_POLICY_MAPPINGS.md)
   - Create test cases for each role
   - Verify expected access/denial

2. **Coverage Testing**:
   - Ensure all 100+ endpoints are tested
   - Check all 7 roles
   - Test both ALLOW and DENY cases

---

## 📊 Statistics

### Endpoints
```
Total Endpoints:      100+
By Service:
  Auth Service:       25+ endpoints
  Payment Service:    35+ endpoints
  Employer Service:   40+ endpoints

HTTP Methods:
  GET:                45 endpoints (read)
  POST:               25 endpoints (create)
  PUT:                20 endpoints (update)
  DELETE:             12 endpoints (delete)
```

### Policy-Capability Mappings
```
Total Roles:         7
Total Capabilities:  98
Total Links:         288
Average per Role:    41 capabilities

Role Distribution:
  PLATFORM_BOOTSTRAP: 55 capabilities (56%)
  ADMIN_TECH:         51 capabilities (52%)
  ADMIN_OPS:          42 capabilities (43%)
  BOARD:              17 capabilities (17%)
  EMPLOYER:           19 capabilities (19%)
  WORKER:             14 capabilities (14%)
  TEST_USER:          50 capabilities (51%)
```

---

## 🔗 Related Documentation

- **Architecture**: [../ARCHITECTURE.md](../ARCHITECTURE.md)
- **Roles**: [../ROLES.md](../ROLES.md)
- **Capabilities**: [../DEFINITIONS/PHASE3_CAPABILITIES_DEFINITION.md](../DEFINITIONS/PHASE3_CAPABILITIES_DEFINITION.md)
- **UI Pages**: [../DEFINITIONS/PHASE2_UI_PAGES_ACTIONS.md](../DEFINITIONS/PHASE2_UI_PAGES_ACTIONS.md)

---

## ✅ Verification Checklist

Before finalizing mappings:
- [ ] All 100+ endpoints documented in PHASE1
- [ ] All 7 policies defined in PHASE4
- [ ] All 288 links verified in PHASE4
- [ ] All endpoints mapped to roles in PHASE5
- [ ] No endpoints without security policy
- [ ] No duplicate endpoint mappings
- [ ] Capability names match PHASE3 definitions
- [ ] Role names match PHASE4 definitions
- [ ] HTTP methods are correct for each endpoint

---

## 🚀 Implementation Steps

1. **Extract Endpoints** (PHASE1)
   - Document all API endpoints
   - Identify HTTP methods
   - Note authentication requirements

2. **Define Policies** (PHASE4)
   - Create role-based policies
   - Link capabilities to each policy
   - Ensure least privilege

3. **Map Endpoints** (PHASE5)
   - Associate endpoints with policies
   - Specify required capabilities
   - Document access decisions

---

**Next**: Review [../setup/README.md](../setup/README.md) for SQL script documentation.
