package com.sharemoney.menu.repository;

import com.sharemoney.menu.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    boolean existsByMenuKeyIgnoreCase(String menuKey);

    List<MenuItem> findAllByOrderBySortOrderAsc();

    List<MenuItem> findByActiveTrueOrderBySortOrderAsc();

    List<MenuItem> findByParent_Id(Long parentId);
}
