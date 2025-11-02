# Auth Service Documentation

> Complete Role-Based Access Control (RBAC) system documentation for the Payment Reconciliation Auth Service.

**Status**: ✅ Complete & Production-Ready  
**Last Updated**: November 2025  
**Version**: 1.0

---

## 📑 Documentation Index

### 🚀 **Quick Start**
- **[ONBOARDING/README.md](ONBOARDING/README.md)** - Start here! Onboarding checklist and overview
- **[ONBOARDING/SETUP_GUIDE.md](ONBOARDING/SETUP_GUIDE.md)** - Step-by-step setup instructions
- **[ONBOARDING/ROLES.md](ONBOARDING/ROLES.md)** - Complete 7-role system definition

### 🏗️ **Architecture & Design**
- **[ONBOARDING/ARCHITECTURE.md](ONBOARDING/ARCHITECTURE.md)** - Complete system architecture with diagrams
  - System overview and components
  - Three-layer authorization model
  - JWT and token flow
  - VPD data isolation patterns
  - Implementation patterns

### 📊 **RBAC System**

#### Definitions
- **[RBAC/DEFINITIONS/PHASE2_UI_PAGES_ACTIONS.md](RBAC/DEFINITIONS/PHASE2_UI_PAGES_ACTIONS.md)**
  - 36 UI pages organized in 8 groups
  - Page-level actions and capability mappings

- **[RBAC/DEFINITIONS/PHASE3_CAPABILITIES_DEFINITION.md](RBAC/DEFINITIONS/PHASE3_CAPABILITIES_DEFINITION.md)**
  - 98 atomic capabilities
  - `<domain>.<subject>.<action>` naming convention
  - Capability definitions by domain

#### Mappings
- **[RBAC/MAPPINGS/PHASE1_ENDPOINTS_EXTRACTION.md](RBAC/MAPPINGS/PHASE1_ENDPOINTS_EXTRACTION.md)**
  - 100+ endpoints from 3 microservices
  - Endpoints organized by service and category

- **[RBAC/MAPPINGS/PHASE4_POLICY_CAPABILITY_MAPPINGS.md](RBAC/MAPPINGS/PHASE4_POLICY_CAPABILITY_MAPPINGS.md)**
  - 7-role policy definitions
  - 288 policy-capability links
  - Three-layer authorization model

- **[RBAC/MAPPINGS/PHASE5_ENDPOINT_POLICY_MAPPINGS.md](RBAC/MAPPINGS/PHASE5_ENDPOINT_POLICY_MAPPINGS.md)**
  - Endpoint-to-role access matrix
  - 100+ endpoint-policy mappings
  - Role access distribution

#### Implementation
- **[RBAC/README.md](RBAC/README.md)** - RBAC implementation overview
- **[RBAC/setup.md](RBAC/setup.md)** - RBAC setup via API
- **[RBAC/testing.md](RBAC/testing.md)** - RBAC testing procedures
- **[RBAC/troubleshoot.md](RBAC/troubleshoot.md)** - RBAC troubleshooting guide

### 🔒 **VPD (Virtual Private Data)**
- **[VPD/README.md](VPD/README.md)** - VPD overview and architecture
- **[VPD/setup.md](VPD/setup.md)** - VPD implementation guide
- **[VPD/testing.md](VPD/testing.md)** - VPD testing procedures
- **[VPD/troubleshoot.md](VPD/troubleshoot.md)** - VPD troubleshooting guide

### 🗄️ **Database**
- **[POSTGRES/README.md](POSTGRES/README.md)** - PostgreSQL setup and configuration
- **[POSTGRES/setup.md](POSTGRES/setup.md)** - Database initialization
- **[POSTGRES/testing.md](POSTGRES/testing.md)** - Database testing procedures
- **[POSTGRES/troubleshoot.md](POSTGRES/troubleshoot.md)** - Database troubleshooting guide

### 💾 **SQL Scripts**
- **[ONBOARDING/setup/](ONBOARDING/setup/)** - SQL initialization scripts
  - `01_create_roles.sql` - Create 7 roles
  - `02_create_capabilities.sql` - Create 98 capabilities
  - `03_create_policies.sql` - Create 7 policies
  - `04_link_policies_to_capabilities.sql` - Link capabilities to policies (288 links)
  - `05_create_seed_users.sql` - Create seed users
  - `06_assign_users_to_roles.sql` - Assign users to roles
  - `07_verify_setup.sql` - Verify setup
  - `08_configure_vpd.sql` - Configure VPD with correct schema
  - `README.md` - SQL scripts documentation

---

## 🎯 **System Overview**

### Key Metrics
```
Total Roles               : 7
Total Capabilities        : 98
Total Endpoints          : 100+
Total UI Pages           : 36
Policy-Capability Links  : 288
Authentication Type      : JWT
Data Isolation Type      : VPD/RLS
```

### 7 Roles
1. **PLATFORM_BOOTSTRAP** (55/98 capabilities) - System bootstrap
2. **ADMIN_TECH** (51/98 capabilities) - Technical administration
3. **ADMIN_OPS** (42/98 capabilities) - Operational administration
4. **BOARD** (17/98 capabilities) - Board-level management
5. **EMPLOYER** (19/98 capabilities) - Employer management
6. **WORKER** (14/98 capabilities) - Worker/end-user
7. **TEST_USER** (50/98 capabilities) - QA testing

### Three-Layer Authorization Model
```
┌─ Layer 1: ENDPOINT-POLICY ─┐
│   Route-level access      │
└─────────────┬──────────────┘
              │
┌─────────────▼────────────────┐
│ Layer 2: CAPABILITY-POLICY  │
│ Fine-grained permissions     │
└─────────────┬────────────────┘
              │
┌─────────────▼──────────────┐
│ Layer 3: VPD/RLS DATA      │
│ Row-level data isolation   │
└────────────────────────────┘
```

---

## 🚀 **Getting Started**

### 1. **Initial Setup** (First Time)
```
1. Run ONBOARDING/setup/01-07_*.sql scripts
2. Create seed users (included in scripts)
3. Assign roles to users (included in scripts)
4. Configure VPD (08_configure_vpd.sql)
5. Run verification queries (07_verify_setup.sql)
```

### 2. **Deploy System**
```
1. Review ONBOARDING/SETUP_GUIDE.md
2. Execute SQL scripts in order
3. Validate with verification queries
4. Configure Spring Security (see ARCHITECTURE.md)
5. Deploy to target environment
```

### 3. **Test System**
```
1. Test JWT authentication
2. Verify role-based access (Layer 1)
3. Check capability verification (Layer 2)
4. Validate VPD filters (Layer 3)
5. Run integration tests
```

---

## 📚 **Documentation Map**

```
docs/
├── README.md ........................... This file (Main index)
│
├── ONBOARDING/
│   ├── README.md ....................... Onboarding overview
│   ├── ROLES.md ........................ 7-role definitions
│   ├── ARCHITECTURE.md ................. System architecture & diagrams
│   ├── SETUP_GUIDE.md .................. Setup instructions
│   └── setup/
│       ├── README.md ................... SQL scripts documentation
│       ├── 01_create_roles.sql ......... Create 7 roles
│       ├── 02_create_capabilities.sql . Create 98 capabilities
│       ├── 03_create_policies.sql ..... Create 7 policies
│       ├── 04_link_policies_to_capabilities.sql ... 288 links
│       ├── 05_create_seed_users.sql ... Create seed users
│       ├── 06_assign_users_to_roles.sql . User-role assignments
│       ├── 07_verify_setup.sql ........ Verification queries
│       └── 08_configure_vpd.sql ....... VPD configuration
│
├── RBAC/
│   ├── README.md ....................... RBAC overview
│   ├── setup.md ........................ RBAC API setup
│   ├── testing.md ...................... RBAC testing guide
│   ├── troubleshoot.md ................. RBAC troubleshooting
│   ├── DEFINITIONS/
│   │   ├── PHASE2_UI_PAGES_ACTIONS.md . 36 UI pages (8 groups)
│   │   └── PHASE3_CAPABILITIES_DEFINITION.md . 98 capabilities
│   └── MAPPINGS/
│       ├── PHASE1_ENDPOINTS_EXTRACTION.md . 100+ endpoints
│       ├── PHASE4_POLICY_CAPABILITY_MAPPINGS.md . 7 policies
│       └── PHASE5_ENDPOINT_POLICY_MAPPINGS.md ... Endpoint matrix
│
├── VPD/
│   ├── README.md ....................... VPD overview
│   ├── setup.md ........................ VPD implementation
│   ├── testing.md ...................... VPD testing
│   ├── troubleshoot.md ................. VPD troubleshooting
│   └── testing/
│       ├── test_rls_basic.sql
│       ├── test_rls_comparison.sql
│       └── test_acl_matrix.sql
│
└── POSTGRES/
    ├── README.md ....................... PostgreSQL overview
    ├── setup.md ........................ PostgreSQL setup
    ├── testing.md ...................... PostgreSQL testing
    ├── troubleshoot.md ................. PostgreSQL troubleshooting
    └── setup/
        ├── reset_all_sequences.sql
        └── reset-sequences.sh
```

---

## ✅ **Verification Checklist**

Before deploying to production, verify:

- ✅ All 7 roles created successfully
- ✅ All 98 capabilities defined
- ✅ All 7 policies linked to capabilities (288 links)
- ✅ All seed users created
- ✅ User-role assignments complete
- ✅ VPD configuration applied
- ✅ JWT authentication working
- ✅ Three-layer authorization functioning
- ✅ Row-level security enforced
- ✅ Endpoint policies mapped
- ✅ UI page access controls verified
- ✅ Integration tests passing

Run `07_verify_setup.sql` to validate all items.

---

## 📖 **Document Cross-References**

### Architecture & Security
- **Overall Design**: See [ARCHITECTURE.md](ONBOARDING/ARCHITECTURE.md)
- **Role Definitions**: See [ROLES.md](ONBOARDING/ROLES.md)
- **Three-Layer Model**: See [ARCHITECTURE.md](ONBOARDING/ARCHITECTURE.md#three-layer-authorization-model)
- **VPD Patterns**: See [ARCHITECTURE.md](ONBOARDING/ARCHITECTURE.md#data-isolation-vpd)

### Implementation
- **Setup Steps**: See [SETUP_GUIDE.md](ONBOARDING/SETUP_GUIDE.md)
- **SQL Scripts**: See [ONBOARDING/setup/README.md](ONBOARDING/setup/README.md)
- **RBAC Setup**: See [RBAC/setup.md](RBAC/setup.md)
- **VPD Setup**: See [VPD/setup.md](VPD/setup.md)

### Testing & Troubleshooting
- **Verify Setup**: Run [07_verify_setup.sql](ONBOARDING/setup/07_verify_setup.sql)
- **RBAC Issues**: See [RBAC/troubleshoot.md](RBAC/troubleshoot.md)
- **VPD Issues**: See [VPD/troubleshoot.md](VPD/troubleshoot.md)
- **Database Issues**: See [POSTGRES/troubleshoot.md](POSTGRES/troubleshoot.md)

---

## 🔄 **Document History**

| Phase | Title | Status | Date |
|-------|-------|--------|------|
| 1 | Extract 100+ Endpoints | ✅ Complete | Nov 2025 |
| 2 | Define 36 UI Pages | ✅ Complete | Nov 2025 |
| 3 | Define 98 Capabilities | ✅ Complete | Nov 2025 |
| 4 | Create Policy Mappings | ✅ Complete | Nov 2025 |
| 5 | Create Endpoint Mappings | ✅ Complete | Nov 2025 |
| 6 | Update ROLES.md | ✅ Complete | Nov 2025 |
| 7 | Refactor setup.md | ✅ Complete | Nov 2025 |
| 8 | Generate SQL Scripts | ✅ Complete | Nov 2025 |
| 8b | Create VPD Script | ✅ Complete | Nov 2025 |
| 9 | Create ARCHITECTURE.md | ✅ Complete | Nov 2025 |
| 10 | Final Review & Organization | ✅ Complete | Nov 2025 |

---

## 📞 **Support**

### Common Questions

**Q: Where do I start?**  
A: Read [ONBOARDING/README.md](ONBOARDING/README.md) first, then follow [ONBOARDING/SETUP_GUIDE.md](ONBOARDING/SETUP_GUIDE.md).

**Q: How does the authorization work?**  
A: See [ONBOARDING/ARCHITECTURE.md](ONBOARDING/ARCHITECTURE.md#three-layer-authorization-model) for the three-layer model.

**Q: What are the 7 roles?**  
A: See [ONBOARDING/ROLES.md](ONBOARDING/ROLES.md) for complete role definitions.

**Q: How do I troubleshoot issues?**  
A: See the troubleshoot.md files in RBAC/, VPD/, and POSTGRES/ folders.

**Q: Where are the SQL scripts?**  
A: All scripts are in [ONBOARDING/setup/](ONBOARDING/setup/) folder with comprehensive README.

---

## 📝 **Notes**

- This documentation is organized by functional area
- All files cross-reference each other for easy navigation
- SQL scripts are numbered in execution order (01-08)
- All unnecessary/duplicate docs have been removed
- Use this README.md as your navigation hub

---

**Next Steps**: Begin with [ONBOARDING/README.md](ONBOARDING/README.md) for onboarding checklist.

**Questions?** Refer to the troubleshooting guides in each folder.
