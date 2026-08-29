package com.sharemoney.menu.repository;

import com.sharemoney.common.domain.UserRole;

public interface MenuPermissionProjection {

    Long getMenuItemId();

    UserRole getRole();
}
