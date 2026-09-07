package com.sharemoney.debt.repository;

import com.sharemoney.debt.entity.OpenLoanRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OpenLoanRecordRepository extends JpaRepository<OpenLoanRecord, Long> {

    List<OpenLoanRecord> findByDebt_IdIn(List<Long> debtIds);
}
