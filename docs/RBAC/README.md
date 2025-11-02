# RBAC System Documentation

> Complete Role-Based Access Control (RBAC) system documentation with architecture, capabilities, policies, endpoints, and implementation guides.

**Status**: Complete & Reviewed  
**Last Updated**: November 2025  
**Version**: 1.0

---

## 📚 Documentation Structure

```
RBAC/
├── ARCHITECTURE.md              # Overall system architecture with visuals
├── ROLES.md                     # Comprehensive role definitions
├── SETUP_GUIDE.md               # System initialization guide
│
├── DEFINITIONS/                 # Capability & UI definitions
│   ├── PHASE2_UI_PAGES_ACTIONS.md    # 36 UI pages organized in 8 groups
│   ├── PHASE3_CAPABILITIES_DEFINITION.md # 98 atomic capabilities
│   └── README.md                      # Navigation guide
│
├── MAPPINGS/                    # Endpoint & policy mappings
│   ├── PHASE1_ENDPOINTS_EXTRACTION.md       # 100+ endpoints from 3 services
│   ├── PHASE4_POLICY_CAPABILITY_MAPPINGS.md # 7 policies with 288 links
│   ├── PHASE5_ENDPOINT_POLICY_MAPPINGS.md   # Endpoint-role access matrix
│   └── README.md                            # Navigation guide
│
├── setup/                       # SQL initialization scripts
│   ├── 01_create_roles.sql                  # Create 7 roles
│   ├── 02_create_capabilities.sql           # Create 98 capabilities
│   ├── 03_create_policies.sql               # Create 7 policies
│   ├── 04_link_policies_to_capabilities.sql # Create 288 links
│   ├── 05_create_seed_users.sql             # Create 7 test users
│   ├── 06_assign_users_to_roles.sql         # Create user-role links
│   ├── 07_verify_setup.sql                  # Verification queries
│   ├── 08_configure_vpd.sql                 # VPD configuration
│   └── README.md                            # SQL script documentation
│
├── testing.md                   # Testing procedures & queries
├── troubleshoot.md              # Troubleshooting guide
└── README.md                    # This file
```

---

## 🚀 Quick Start

### For New Users
1. **Read First**: [ROLES.md](ROLES.md) - Understand the 7 roles and their capabilities
2. **Understand System**: [ARCHITECTURE.md](ARCHITECTURE.md) - Three-layer authorization model
3. **Setup System**: [SETUP_GUIDE.md](SETUP_GUIDE.md) - Initialize the RBAC system
4. **Run Scripts**: [setup/README.md](setup/README.md) - Execute SQL scripts in order

### For Developers
1. **Understand Capabilities**: [DEFINITIONS/](DEFINITIONS/) - 98 capabilities defined
2. **Review Endpoints**: [MAPPINGS/PHASE1_ENDPOINTS_EXTRACTION.md](MAPPINGS/PHASE1_ENDPOINTS_EXTRACTION.md) - 100+ endpoints
3. **Check Policies**: [MAPPINGS/PHASE5_ENDPOINT_POLICY_MAPPINGS.md](MAPPINGS/PHASE5_ENDPOINT_POLICY_MAPPINGS.md) - Access matrix
4. **Test Implementation**: [testing.md](testing.md) - Test procedures

### For Administrators
1. **System Overview**: [ARCHITECTURE.md](ARCHITECTURE.md) - System design
2. **Role Reference**: [ROLES.md](ROLES.md) - Role definitions
3. **Setup & Config**: [SETUP_GUIDE.md](SETUP_GUIDE.md) - Configuration steps
4. **Troubleshooting**: [troubleshoot.md](troubleshoot.md) - Common issues

---

## 📋 Complete Deliverables

### Documentation Files

| File | Purpose | Audience |
|------|---------|----------|
| ARCHITECTURE.md | System design with visuals | Architects, Leads |
| ROLES.md | Role definitions & responsibilities | Everyone |
| SETUP_GUIDE.md | System initialization guide | DevOps, Admins |
| DEFINITIONS/ | Capabilities & UI pages | Developers |
| MAPPINGS/ | Endpoints & policy mappings | Developers, Leads |
| testing.md | Testing procedures | QA, Developers |
| troubleshoot.md | Common issues & solutions | Developers, Admins |

### SQL Scripts

| Script | Purpose | Execution Order |
|--------|---------|-----------------|
| 01_create_roles.sql | Create 7 RBAC roles | 1st |
| 02_create_capabilities.sql | Create 98 capabilities | 2nd |
| 03_create_policies.sql | Create 7 policies | 3rd |
| 04_link_policies_to_capabilities.sql | Create 288 policy-capability links | 4th |
| 05_create_seed_users.sql | Create 7 test users | 5th |
| 06_assign_users_to_roles.sql | Create user-role assignments | 6th |
| 07_verify_setup.sql | Verify all setup | 7th |
| 08_configure_vpd.sql | Configure VPD/RLS | 8th |

---

## 🎯 System Overview

### 7 Roles Implemented

```
PLATFORM_BOOTSTRAP     55/98 capabilities (56%)  - System bootstrap only
ADMIN_TECH             51/98 capabilities (52%)  - Technical administration
ADMIN_OPS              42/98 capabilities (43%)  - Operations administration
BOARD                  17/98 capabilities (17%)  - Board-level management
EMPLOYER               19/98 capabilities (19%)  - Employer-level management
WORKER                 14/98 capabilities (14%)  - User-level access
TEST_USER              50/98 capabilities (51%)  - QA testing
```

### Authorization Model

**Three-Layer Authorization:**
1. **Layer 1**: Endpoint-Policy Authorization (route-level access)
2. **Layer 2**: Capability-Policy Authorization (fine-grained permissions)
3. **Layer 3**: Data Isolation (VPD/RLS at database level)

### Key Features

- ✅ **98 Atomic Capabilities** with `<domain>.<subject>.<action>` naming
- ✅ **100+ Endpoints** mapped to role policies
- ✅ **36 UI Pages** organized in 8 groups
- ✅ **288 Policy-Capability Links** for fine-grained control
- ✅ **JWT Authentication** with secure token validation
- ✅ **VPD Configuration** for row-level data isolation
- ✅ **Spring Security Integration** with @PreAuthorize annotations

---

## 📁 Folder Organization

### DEFINITIONS/
Contains capability and UI page definitions:
- **PHASE2_UI_PAGES_ACTIONS.md** - 36 UI pages in 8 groups
- **PHASE3_CAPABILITIES_DEFINITION.md** - 98 capabilities defined

### MAPPINGS/
Contains endpoint and policy mappings:
- **PHASE1_ENDPOINTS_EXTRACTION.md** - 100+ endpoints from 3 services
- **PHASE4_POLICY_CAPABILITY_MAPPINGS.md** - 7 policies with capabilities
- **PHASE5_ENDPOINT_POLICY_MAPPINGS.md** - Endpoint-role access matrix

### setup/
Contains SQL initialization scripts:
- **01-07**: Core RBAC setup (roles, capabilities, policies, users)
- **08**: VPD configuration for data isolation
- **README.md**: Script execution guide

---

## 🔍 Key Concepts

### Capabilities
Format: `<domain>.<subject>.<action>`

Example: `payment.record.create` means "user can create payment records"

Domains: user, payment, report, employer, worker, board, system, audit, etc.

### Roles
Hierarchical structure from bootstrap (all permissions) → specialized roles

Each role has specific capabilities and endpoint access.

### Policies
Policies link roles to capabilities and endpoints.

Each policy defines what a role can do.

### VPD (Virtual Private Data)
Row-level security configuration at database level.

Users see only data they're authorized for based on:
- User ID
- Board ID
- Employer ID
- Read/Write permissions

---

## 🚀 Getting Started

### 1. Initialize Database
```bash
cd setup/
psql -U postgres -d auth_db -f 01_create_roles.sql
psql -U postgres -d auth_db -f 02_create_capabilities.sql
psql -U postgres -d auth_db -f 03_create_policies.sql
psql -U postgres -d auth_db -f 04_link_policies_to_capabilities.sql
psql -U postgres -d auth_db -f 05_create_seed_users.sql
psql -U postgres -d auth_db -f 06_assign_users_to_roles.sql
psql -U postgres -d auth_db -f 07_verify_setup.sql
psql -U postgres -d auth_db -f 08_configure_vpd.sql
```

### 2. Verify Setup
Check [testing.md](testing.md) for verification queries.

### 3. Configure Application
Update `application.yml` with database credentials.

### 4. Deploy
Deploy the application with RBAC enabled.

---

## 📚 Documentation References

### System Architecture
- [ARCHITECTURE.md](ARCHITECTURE.md) - Complete system design with visuals

### Role Reference
- [ROLES.md](ROLES.md) - Detailed role descriptions and responsibilities

### Capabilities
- [DEFINITIONS/PHASE3_CAPABILITIES_DEFINITION.md](DEFINITIONS/PHASE3_CAPABILITIES_DEFINITION.md) - All 98 capabilities

### UI Pages
- [DEFINITIONS/PHASE2_UI_PAGES_ACTIONS.md](DEFINITIONS/PHASE2_UI_PAGES_ACTIONS.md) - All 36 UI pages

### Endpoints
- [MAPPINGS/PHASE1_ENDPOINTS_EXTRACTION.md](MAPPINGS/PHASE1_ENDPOINTS_EXTRACTION.md) - All 100+ endpoints

### Policy Mappings
- [MAPPINGS/PHASE4_POLICY_CAPABILITY_MAPPINGS.md](MAPPINGS/PHASE4_POLICY_CAPABILITY_MAPPINGS.md) - Policies & capabilities

### Endpoint Access Matrix
- [MAPPINGS/PHASE5_ENDPOINT_POLICY_MAPPINGS.md](MAPPINGS/PHASE5_ENDPOINT_POLICY_MAPPINGS.md) - Who can access what

---

## ✅ Verification Checklist

- [ ] All 7 roles created (01_create_roles.sql)
- [ ] All 98 capabilities created (02_create_capabilities.sql)
- [ ] All 7 policies created (03_create_policies.sql)
- [ ] All 288 policy-capability links created (04_link_policies_to_capabilities.sql)
- [ ] All 7 test users created (05_create_seed_users.sql)
- [ ] All user-role assignments created (06_assign_users_to_roles.sql)
- [ ] Verification queries pass (07_verify_setup.sql)
- [ ] VPD configuration applied (08_configure_vpd.sql)
- [ ] JWT authentication working
- [ ] @PreAuthorize annotations functioning
- [ ] Row-level security filtering data correctly

---

## 🔗 Related Documentation

- **Architecture**: [ARCHITECTURE.md](ARCHITECTURE.md)
- **Roles**: [ROLES.md](ROLES.md)
- **Setup**: [SETUP_GUIDE.md](SETUP_GUIDE.md)
- **Testing**: [testing.md](testing.md)
- **Troubleshooting**: [troubleshoot.md](troubleshoot.md)
- **SQL Scripts**: [setup/README.md](setup/README.md)

---

## 📞 Support

For questions or issues:
1. Check [troubleshoot.md](troubleshoot.md)
2. Review [testing.md](testing.md) for test procedures
3. Consult [ROLES.md](ROLES.md) for role clarifications
4. Review [ARCHITECTURE.md](ARCHITECTURE.md) for system design questions

---

**Next Steps**: Follow [SETUP_GUIDE.md](SETUP_GUIDE.md) to initialize the system.
