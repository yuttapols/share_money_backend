package com.sharemoney.menu.controller;

import com.sharemoney.common.response.ApiResponse;
import com.sharemoney.menu.dto.AdminMenuResponse;
import com.sharemoney.menu.dto.MenuCreateRequest;
import com.sharemoney.menu.dto.MenuPermissionRequest;
import com.sharemoney.menu.dto.MenuPermissionResponse;
import com.sharemoney.menu.dto.MenuUpdateRequest;
import com.sharemoney.menu.service.AdminMenuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/menus")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminMenuController {

    private final AdminMenuService adminMenuService;

    @GetMapping
    public ApiResponse<List<AdminMenuResponse>> list() {
        return ApiResponse.success(adminMenuService.listAll());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AdminMenuResponse>> create(@Valid @RequestBody MenuCreateRequest request) {
        AdminMenuResponse response = adminMenuService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Menu item has been created successfully."));
    }

    @PutMapping("/{id}")
    public ApiResponse<AdminMenuResponse> update(@PathVariable Long id, @Valid @RequestBody MenuUpdateRequest request) {
        return ApiResponse.success(adminMenuService.update(id, request), "Data has been saved successfully.");
    }

    @PutMapping("/{id}/permissions")
    public ApiResponse<MenuPermissionResponse> updatePermissions(@PathVariable Long id, @Valid @RequestBody MenuPermissionRequest request) {
        return ApiResponse.success(adminMenuService.updatePermissions(id, request), "Data has been saved successfully.");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        adminMenuService.delete(id);
        return ApiResponse.success(null, "Menu item has been deleted successfully.");
    }
}
