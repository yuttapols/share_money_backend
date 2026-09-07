package com.sharemoney.debt.repository;

import com.sharemoney.debt.entity.Installment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InstallmentRepository extends JpaRepository<Installment, Long> {

    List<Installment> findByDebt_IdIn(List<Long> debtIds);
}
