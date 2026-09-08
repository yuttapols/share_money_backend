package com.sharemoney.profile.repository;

import com.sharemoney.profile.entity.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    List<BankAccount> findAllByCreditor_IdOrderByIdAsc(Long creditorId);

    Optional<BankAccount> findByIdAndCreditor_Id(Long id, Long creditorId);
}
