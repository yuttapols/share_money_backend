package com.sharemoney.slip.repository;

import com.sharemoney.slip.entity.Slip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SlipRepository extends JpaRepository<Slip, Long> {

    List<Slip> findByDebtor_IdAndCreditor_IdOrderByUploadedAtDesc(Long debtorId, Long creditorId);

    @Query("select distinct s.debtor.username from Slip s where s.creditor.id = :creditorId")
    List<String> findDebtorUsernamesWithSlipByCreditor(@Param("creditorId") Long creditorId);

    @Query("select distinct s.debtor.username from Slip s")
    List<String> findAllDebtorUsernamesWithSlip();
}
