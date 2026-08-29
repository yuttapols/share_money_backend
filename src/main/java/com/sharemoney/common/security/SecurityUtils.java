package com.sharemoney.common.security;

import com.sharemoney.common.domain.UserRole;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static UserPrincipal currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new AccessDeniedException("Authentication is required to access this resource.");
        }
        return principal;
    }

    public static Long currentUserId() {
        return currentUser().getId();
    }

    public static String currentUsername() {
        return currentUser().getUsername();
    }

    public static UserRole currentRole() {
        return currentUser().getRole();
    }

    public static boolean isAdmin() {
        return currentRole() == UserRole.ADMIN;
    }
}
