# Capability & UI Definitions

> Comprehensive definitions of all 98 atomic capabilities and 36 UI pages in the RBAC system.

---

## 📋 Contents

### 1. UI Pages & Actions
**File**: [PHASE2_UI_PAGES_ACTIONS.md](PHASE2_UI_PAGES_ACTIONS.md)

**Overview**: 
- 36 total UI pages
- Organized in 8 logical groups
- Each page lists associated user actions
- Actions mapped to capabilities

**Groups Covered**:
1. Dashboard Pages (4 pages)
2. User Management (5 pages)
3. Payment Processing (6 pages)
4. Reporting & Analytics (5 pages)
5. Configuration (4 pages)
6. Employer Management (4 pages)
7. Worker Management (4 pages)
8. System Administration (4 pages)

---

### 2. Atomic Capabilities
**File**: [PHASE3_CAPABILITIES_DEFINITION.md](PHASE3_CAPABILITIES_DEFINITION.md)

**Overview**:
- 98 total capabilities
- Naming format: `<domain>.<subject>.<action>`
- Fine-grained, reusable permissions
- Mapped to 7 roles

**Domains Covered**:
- `user.*` - User management capabilities
- `payment.*` - Payment processing capabilities
- `employer.*` - Employer management capabilities
- `worker.*` - Worker management capabilities
- `board.*` - Board operations capabilities
- `report.*` - Reporting capabilities
- `system.*` - System administration capabilities
- `audit.*` - Audit and compliance capabilities

---

## 🔍 How to Use

### For Developers
1. **Understanding Capabilities**: 
   - Read [PHASE3_CAPABILITIES_DEFINITION.md](PHASE3_CAPABILITIES_DEFINITION.md)
   - Find the capability format and naming convention
   - Map capabilities to your API endpoints

2. **UI Implementation**:
   - Review [PHASE2_UI_PAGES_ACTIONS.md](PHASE2_UI_PAGES_ACTIONS.md)
   - Identify which pages need capability guards
   - Apply @PreAuthorize with capability names

### For Business Analysts
1. **Page Organization**:
   - Check [PHASE2_UI_PAGES_ACTIONS.md](PHASE2_UI_PAGES_ACTIONS.md)
   - Understand user workflows per page
   - Identify missing or redundant pages

2. **Capability Review**:
   - Study [PHASE3_CAPABILITIES_DEFINITION.md](PHASE3_CAPABILITIES_DEFINITION.md)
   - Verify all user actions are covered
   - Add new capabilities as needed

### For Architects
1. **System Design**:
   - Review capability taxonomy in [PHASE3_CAPABILITIES_DEFINITION.md](PHASE3_CAPABILITIES_DEFINITION.md)
   - Check naming convention consistency
   - Evaluate capability granularity

2. **Policy Design**:
   - Map roles to capabilities
   - Ensure least privilege principles
   - Plan VPD integration

---

## 📊 Statistics

### UI Pages
```
Total Pages:     36
Groups:          8
Avg per Group:   4-5 pages

Largest Group:   Payment Processing (6 pages)
Smallest Group:  Multiple groups (4 pages each)
```

### Capabilities
```
Total Capabilities:  98
Domains:             8
Actions per Domain:  12 average

Most Common Action:  .view (21 capabilities)
Second Most:        .create (19 capabilities)
Third Most:         .update (17 capabilities)
```

---

## 🔗 Related Documentation

- **Architecture**: [../ARCHITECTURE.md](../ARCHITECTURE.md)
- **Roles**: [../ROLES.md](../ROLES.md)
- **Policy Mappings**: [../MAPPINGS/PHASE4_POLICY_CAPABILITY_MAPPINGS.md](../MAPPINGS/PHASE4_POLICY_CAPABILITY_MAPPINGS.md)
- **Endpoint Mappings**: [../MAPPINGS/PHASE5_ENDPOINT_POLICY_MAPPINGS.md](../MAPPINGS/PHASE5_ENDPOINT_POLICY_MAPPINGS.md)

---

## ✅ Verification

Before using capabilities and UI pages:
- [ ] All 36 UI pages documented
- [ ] All 98 capabilities defined
- [ ] Naming convention consistent (`<domain>.<subject>.<action>`)
- [ ] Capabilities mapped to roles (see [../MAPPINGS/](../MAPPINGS/))
- [ ] UI actions mapped to capabilities
- [ ] No duplicate capability names
- [ ] No orphaned capabilities (unused in any role)

---

**Next**: Review [../MAPPINGS/README.md](../MAPPINGS/README.md) for endpoint and policy mappings.
