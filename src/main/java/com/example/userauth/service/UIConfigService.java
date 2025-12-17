package com.example.userauth.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.example.userauth.dto.AuthorizationMatrix;
import com.example.userauth.dto.PermissionResponse;
import com.example.userauth.dto.PermissionResponse.NavigationItem;
import com.example.userauth.entity.User;

@Service
public class UIConfigService {

    private final AuthService authService;
    private final AuthorizationService authorizationService;

    public UIConfigService(AuthService authService, AuthorizationService authorizationService) {
        this.authService = authService;
        this.authorizationService = authorizationService;
    }

    /**
     * Get complete UI configuration for the current user
     */
    public PermissionResponse getUserUIConfig() {
        Optional<User> currentUser = authService.getCurrentUser();
        if (currentUser.isEmpty()) {
            return null;
        }
        User user = currentUser.get();
        AuthorizationMatrix matrix = authorizationService.buildAuthorizationMatrix(user.getId());
        Set<String> roles = matrix.getRoles();
        Set<String> policies = matrix.getPolicies();

        // Generate navigation based on policies
        List<NavigationItem> navigation = generateNavigation(policies);

        // Generate UI configuration
        Map<String, Object> uiConfig = generateUIConfig(roles, policies);

        return new PermissionResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                roles,
                policies,
                uiConfig,
                navigation);
    }

    /**
     * Generate navigation structure based on user permissions
     */
    private List<NavigationItem> generateNavigation(Set<String> policies) {
        List<NavigationItem> navigation = new ArrayList<>();

        // Dashboard - Always visible for authenticated users
        navigation.add(new NavigationItem(
                "dashboard", "Dashboard", "/dashboard", "🏠", "main", Set.of()));

        // Administration Section
        if (policies.contains("MANAGE_USERS") || policies.contains("SYSTEM_MAINTENANCE")) {
            NavigationItem adminSection = new NavigationItem(
                    "admin", "Administration", "", "⚙️", "admin", Set.of("MANAGE_USERS", "SYSTEM_MAINTENANCE"));

            List<NavigationItem> adminChildren = new ArrayList<>();

            if (policies.contains("MANAGE_USERS")) {
                adminChildren.add(new NavigationItem(
                        "admin-users", "User Management", "/admin/users", "👥", "admin", Set.of("MANAGE_USERS")));
            }

            if (policies.contains("SYSTEM_MAINTENANCE")) {
                adminChildren.add(new NavigationItem(
                        "admin-system", "System Settings", "/admin/system", "🔧", "admin",
                        Set.of("SYSTEM_MAINTENANCE")));
            }

            if (policies.contains("VIEW_SYSTEM_LOGS")) {
                adminChildren.add(new NavigationItem(
                        "admin-logs", "System Logs", "/admin/logs", "📋", "admin", Set.of("VIEW_SYSTEM_LOGS")));
            }

            adminSection.setChildren(adminChildren);
            navigation.add(adminSection);
        }

        // Reconciliation Section
        if (policies.contains("PERFORM_RECONCILIATION") || policies.contains("PROCESS_PAYMENTS")) {
            NavigationItem reconSection = new NavigationItem(
                    "reconciliation", "Reconciliation", "", "💰", "reconciliation",
                    Set.of("PERFORM_RECONCILIATION", "PROCESS_PAYMENTS"));

            List<NavigationItem> reconChildren = new ArrayList<>();

            if (policies.contains("PERFORM_RECONCILIATION")) {
                reconChildren.add(new NavigationItem(
                        "recon-dashboard", "Reconciliation Dashboard", "/reconciliation", "📊", "reconciliation",
                        Set.of("PERFORM_RECONCILIATION")));
            }

            if (policies.contains("PROCESS_PAYMENTS")) {
                reconChildren.add(new NavigationItem(
                        "payment-processing", "Payment Processing", "/payments/process", "💳", "reconciliation",
                        Set.of("PROCESS_PAYMENTS")));
            }

            reconSection.setChildren(reconChildren);
            navigation.add(reconSection);
        }

        // Worker Section
        if (policies.contains("READ_WORKER_DATA") || policies.contains("UPLOAD_WORKER_DATA")) {
            NavigationItem workerSection = new NavigationItem(
                    "worker", "Worker Management", "", "👷", "worker",
                    Set.of("READ_WORKER_DATA", "UPLOAD_WORKER_DATA"));

            List<NavigationItem> workerChildren = new ArrayList<>();

            if (policies.contains("READ_WORKER_DATA")) {
                workerChildren.add(new NavigationItem(
                        "worker-payments", "Worker Payments", "/worker/payments", "💵", "worker",
                        Set.of("READ_WORKER_DATA")));
            }

            if (policies.contains("UPLOAD_WORKER_DATA")) {
                workerChildren.add(new NavigationItem(
                        "worker-upload", "Upload Worker Data", "/worker/upload", "📁", "worker",
                        Set.of("UPLOAD_WORKER_DATA")));
            }

            workerSection.setChildren(workerChildren);
            navigation.add(workerSection);
        }

        // Employer Section
        if (policies.contains("READ_EMPLOYER_RECEIPTS") || policies.contains("VALIDATE_EMPLOYER_RECEIPTS")) {
            NavigationItem employerSection = new NavigationItem(
                    "employer", "Employer Management", "", "🏢", "employer",
                    Set.of("READ_EMPLOYER_RECEIPTS", "VALIDATE_EMPLOYER_RECEIPTS"));

            List<NavigationItem> employerChildren = new ArrayList<>();

            if (policies.contains("READ_EMPLOYER_RECEIPTS")) {
                employerChildren.add(new NavigationItem(
                        "employer-receipts", "Employer Receipts", "/employer/receipts", "🧾", "employer",
                        Set.of("READ_EMPLOYER_RECEIPTS")));
            }

            employerSection.setChildren(employerChildren);
            navigation.add(employerSection);
        }

        // Board Section
        if (policies.contains("READ_BOARD_RECEIPTS") || policies.contains("APPROVE_BOARD_RECEIPTS")) {
            NavigationItem boardSection = new NavigationItem(
                    "board", "Board Management", "", "📋", "board",
                    Set.of("READ_BOARD_RECEIPTS", "APPROVE_BOARD_RECEIPTS"));

            List<NavigationItem> boardChildren = new ArrayList<>();

            if (policies.contains("READ_BOARD_RECEIPTS")) {
                boardChildren.add(new NavigationItem(
                        "board-receipts", "Board Receipts", "/board/receipts", "📄", "board",
                        Set.of("READ_BOARD_RECEIPTS")));
            }

            if (policies.contains("APPROVE_BOARD_RECEIPTS")) {
                boardChildren.add(new NavigationItem(
                        "board-approvals", "Approvals", "/board/approvals", "✅", "board",
                        Set.of("APPROVE_BOARD_RECEIPTS")));
            }

            boardSection.setChildren(boardChildren);
            navigation.add(boardSection);
        }

        // Profile - Always visible
        navigation.add(new NavigationItem(
                "profile", "Profile", "/profile", "👤", "user", Set.of()));

        return navigation;
    }

    /**
     * Generate UI configuration object
     */
    private Map<String, Object> generateUIConfig(Set<String> roles, Set<String> policies) {
        Map<String, Object> config = new HashMap<>();

        // Feature flags based on permissions
        Map<String, Boolean> features = new HashMap<>();
        features.put("canManageUsers", policies.contains("MANAGE_USERS"));
        features.put("canViewSystemLogs", policies.contains("VIEW_SYSTEM_LOGS"));
        features.put("canPerformReconciliation", policies.contains("PERFORM_RECONCILIATION"));
        features.put("canProcessPayments", policies.contains("PROCESS_PAYMENTS"));
        features.put("canUploadFiles", policies.contains("UPLOAD_WORKER_DATA"));
        features.put("canViewReports", policies.contains("GENERATE_PAYMENT_REPORTS"));
        features.put("canApprovePayments", policies.contains("APPROVE_PAYMENTS"));
        features.put("canRejectPayments", policies.contains("REJECT_PAYMENTS"));

        config.put("features", features);

        // UI behavior configuration
        Map<String, Object> behavior = new HashMap<>();
        behavior.put("defaultRoute", getDefaultRoute(policies));
        behavior.put("theme", getThemeForRoles(roles));
        behavior.put("layout", getLayoutForRoles(roles));

        config.put("behavior", behavior);

        // Action permissions for buttons/components
        Map<String, Set<String>> actions = new HashMap<>();
        actions.put("user-management", Set.of("MANAGE_USERS", "MANAGE_ROLES"));
        actions.put("payment-processing", Set.of("PROCESS_PAYMENTS", "APPROVE_PAYMENTS", "REJECT_PAYMENTS"));
        actions.put("reconciliation", Set.of("PERFORM_RECONCILIATION", "READ_RECONCILIATIONS"));
        actions.put("file-upload", Set.of("UPLOAD_WORKER_DATA"));
        actions.put("reporting", Set.of("GENERATE_PAYMENT_REPORTS", "GENERATE_RECONCILIATION_REPORTS"));

        config.put("actions", actions);

        return config;
    }

    private String getDefaultRoute(Set<String> policies) {
        if (policies.contains("MANAGE_USERS"))
            return "/admin/dashboard";
        if (policies.contains("PERFORM_RECONCILIATION"))
            return "/reconciliation";
        if (policies.contains("READ_WORKER_DATA"))
            return "/worker/payments";
        if (policies.contains("READ_EMPLOYER_RECEIPTS"))
            return "/employer/receipts";
        if (policies.contains("READ_BOARD_RECEIPTS"))
            return "/board/receipts";
        return "/dashboard";
    }

    private String getThemeForRoles(Set<String> roles) {
        if (roles.contains("ADMIN"))
            return "admin-theme";
        if (roles.contains("RECONCILIATION_OFFICER"))
            return "reconciliation-theme";
        return "default-theme";
    }

    private String getLayoutForRoles(Set<String> roles) {
        if (roles.contains("ADMIN"))
            return "full-layout";
        return "standard-layout";
    }

}
