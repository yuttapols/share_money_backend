package com.sharemoney.menu.service;

import com.sharemoney.common.domain.UserRole;
import com.sharemoney.common.security.SecurityUtils;
import com.sharemoney.menu.dto.MenuResponse;
import com.sharemoney.menu.entity.MenuItem;
import com.sharemoney.menu.repository.MenuItemRepository;
import com.sharemoney.menu.repository.MenuPermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuItemRepository menuItemRepository;
    private final MenuPermissionRepository menuPermissionRepository;

    @Transactional(readOnly = true)
    public List<MenuResponse> getMenuTreeForCurrentUser() {
        UserRole role = SecurityUtils.currentRole();
        Set<Long> allowedMenuItemIds = menuPermissionRepository.findMenuItemIdsByRole(role);

        List<MenuItem> visibleItems = menuItemRepository.findByActiveTrueOrderBySortOrderAsc().stream()
                .filter(item -> allowedMenuItemIds.contains(item.getId()))
                .toList();

        Map<Long, List<MenuItem>> childrenByParentId = visibleItems.stream()
                .filter(item -> item.getParent() != null)
                .collect(Collectors.groupingBy(item -> item.getParent().getId()));

        return visibleItems.stream()
                .filter(item -> item.getParent() == null)
                .map(item -> toResponse(item, childrenByParentId))
                .toList();
    }

    private MenuResponse toResponse(MenuItem item, Map<Long, List<MenuItem>> childrenByParentId) {
        List<MenuResponse> children = childrenByParentId.getOrDefault(item.getId(), List.of()).stream()
                .map(child -> toResponse(child, childrenByParentId))
                .toList();
        return new MenuResponse(item.getId(), item.getMenuKey(), item.getIcon(), item.getRoute(), item.getSortOrder(), children);
    }
}
