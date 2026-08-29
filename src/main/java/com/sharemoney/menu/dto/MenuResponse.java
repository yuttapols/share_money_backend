package com.sharemoney.menu.dto;

import java.util.List;

public record MenuResponse(
        Long id,
        String menuKey,
        String icon,
        String route,
        int sortOrder,
        List<MenuResponse> children
) {
}
