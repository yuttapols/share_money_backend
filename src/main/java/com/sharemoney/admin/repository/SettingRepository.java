package com.sharemoney.admin.repository;

import com.sharemoney.admin.entity.Setting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettingRepository extends JpaRepository<Setting, String> {
}
