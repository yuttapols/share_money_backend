package com.sharemoney.profile.repository;

import com.sharemoney.profile.entity.Bank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BankRepository extends JpaRepository<Bank, Long> {

    List<Bank> findAllByOrderBySortOrderAsc();
}
