package com.sharemoney.menu.repository;

import com.sharemoney.common.domain.UserRole;
import com.sharemoney.menu.entity.MenuPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface MenuPermissionRepository extends JpaRepository<MenuPermission, Long> {

    @Query("select mp.menuItem.id as menuItemId, mp.role as role from MenuPermission mp")
    List<MenuPermissionProjection> findAllProjections();

    @Query("select mp.menuItem.id from MenuPermission mp where mp.role = :role")
    Set<Long> findMenuItemIdsByRole(@Param("role") UserRole role);

    @Query("select mp.role from MenuPermission mp where mp.menuItem.id = :menuItemId")
    List<UserRole> findRolesByMenuItemId(@Param("menuItemId") Long menuItemId);

    void deleteByMenuItem_Id(Long menuItemId);
}
