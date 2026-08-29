package com.sharemoney.menu.service;

import com.sharemoney.common.domain.UserRole;
import com.sharemoney.common.error.BusinessException;
import com.sharemoney.common.error.ErrorCode;
import com.sharemoney.menu.dto.AdminMenuResponse;
import com.sharemoney.menu.dto.MenuCreateRequest;
import com.sharemoney.menu.dto.MenuPermissionRequest;
import com.sharemoney.menu.dto.MenuPermissionResponse;
import com.sharemoney.menu.dto.MenuUpdateRequest;
import com.sharemoney.menu.entity.MenuItem;
import com.sharemoney.menu.entity.MenuPermission;
import com.sharemoney.menu.repository.MenuItemRepository;
import com.sharemoney.menu.repository.MenuPermissionProjection;
import com.sharemoney.menu.repository.MenuPermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminMenuService {

    private final MenuItemRepository menuItemRepository;
    private final MenuPermissionRepository menuPermissionRepository;

    @Transactional(readOnly = true)
    public List<AdminMenuResponse> listAll() {
        List<MenuItem> items = menuItemRepository.findAllByOrderBySortOrderAsc();
        Map<Long, List<UserRole>> rolesByMenuItemId = menuPermissionRepository.findAllProjections().stream()
                .collect(Collectors.groupingBy(MenuPermissionProjection::getMenuItemId,
                        Collectors.mapping(MenuPermissionProjection::getRole, Collectors.toList())));

        return items.stream()
                .map(item -> toAdminResponse(item, rolesByMenuItemId.getOrDefault(item.getId(), List.of())))
                .toList();
    }

    @Transactional
    public AdminMenuResponse create(MenuCreateRequest request) {
        if (menuItemRepository.existsByMenuKeyIgnoreCase(request.menuKey())) {
            throw new BusinessException(ErrorCode.MENU_KEY_DUPLICATE);
        }

        MenuItem parent = resolveParent(request.parentId());

        MenuItem item = new MenuItem();
        item.setParent(parent);
        item.setMenuKey(request.menuKey());
        item.setIcon(request.icon());
        item.setRoute(request.route());
        item.setSortOrder(request.sortOrder());
        menuItemRepository.save(item);

        savePermissions(item, request.roles());

        return toAdminResponse(item, request.roles());
    }

    @Transactional
    public AdminMenuResponse update(Long id, MenuUpdateRequest request) {
        MenuItem item = getMenuItemOrThrow(id);
        item.setIcon(request.icon());
        item.setRoute(request.route());
        item.setSortOrder(request.sortOrder());
        item.setActive(request.active());

        List<UserRole> roles = menuPermissionRepository.findRolesByMenuItemId(id);
        return toAdminResponse(item, roles);
    }

    @Transactional
    public MenuPermissionResponse updatePermissions(Long id, MenuPermissionRequest request) {
        MenuItem item = getMenuItemOrThrow(id);
        menuPermissionRepository.deleteByMenuItem_Id(id);
        savePermissions(item, request.roles());
        return new MenuPermissionResponse(id, request.roles());
    }

    @Transactional
    public void delete(Long id) {
        MenuItem item = getMenuItemOrThrow(id);
        menuItemRepository.findByParent_Id(id).forEach(child -> child.setParent(null));
        menuItemRepository.delete(item);
    }

    private MenuItem resolveParent(Long parentId) {
        if (parentId == null) {
            return null;
        }
        return getMenuItemOrThrow(parentId);
    }

    private MenuItem getMenuItemOrThrow(Long id) {
        return menuItemRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.MENU_ITEM_NOT_FOUND));
    }

    private void savePermissions(MenuItem item, List<UserRole> roles) {
        List<MenuPermission> permissions = roles.stream()
                .map(role -> {
                    MenuPermission permission = new MenuPermission();
                    permission.setMenuItem(item);
                    permission.setRole(role);
                    return permission;
                })
                .toList();
        menuPermissionRepository.saveAll(permissions);
    }

    private AdminMenuResponse toAdminResponse(MenuItem item, List<UserRole> roles) {
        Long parentId = item.getParent() != null ? item.getParent().getId() : null;
        return new AdminMenuResponse(item.getId(), parentId, item.getMenuKey(), item.getIcon(),
                item.getRoute(), item.getSortOrder(), item.isActive(), roles);
    }
}
