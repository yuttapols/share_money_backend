package com.sharemoney.profile.repository;

import com.sharemoney.profile.entity.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    Optional<BankAccount> findByCreditor_Id(Long creditorId);
}
