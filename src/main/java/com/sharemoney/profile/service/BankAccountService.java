package com.sharemoney.profile.service;

import com.sharemoney.common.domain.UserRole;
import com.sharemoney.common.error.BusinessException;
import com.sharemoney.common.error.ErrorCode;
import com.sharemoney.common.security.SecurityUtils;
import com.sharemoney.profile.dto.BankAccountResponse;
import com.sharemoney.profile.dto.UpdateBankAccountRequest;
import com.sharemoney.profile.entity.BankAccount;
import com.sharemoney.profile.repository.BankAccountRepository;
import com.sharemoney.user.entity.User;
import com.sharemoney.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class BankAccountService {

    private final BankAccountRepository bankAccountRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public BankAccountResponse getMine() {
        Long creditorId = resolveCreditorId();
        return bankAccountRepository.findByCreditor_Id(creditorId)
                .map(this::toResponse)
                .orElseThrow(() -> new BusinessException(ErrorCode.BANK_ACCOUNT_NOT_FOUND));
    }

    @Transactional
    public BankAccountResponse update(UpdateBankAccountRequest request) {
        Long creditorId = SecurityUtils.currentUserId();
        BankAccount account = bankAccountRepository.findByCreditor_Id(creditorId)
                .orElseGet(() -> newAccountFor(creditorId));

        account.setBankName(request.bankName());
        account.setAccountNo(request.accountNo());
        account.setAccountName(request.accountName());
        account.setPaymentNote(request.paymentNote());
        account.setUpdatedAt(Instant.now());
        bankAccountRepository.save(account);

        return toResponse(account);
    }

    private BankAccount newAccountFor(Long creditorId) {
        User creditor = userRepository.findById(creditorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        BankAccount account = new BankAccount();
        account.setCreditor(creditor);
        return account;
    }

    private Long resolveCreditorId() {
        if (SecurityUtils.currentRole() != UserRole.DEBTOR) {
            return SecurityUtils.currentUserId();
        }
        User debtor = userRepository.findById(SecurityUtils.currentUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (debtor.getCreditor() == null) {
            throw new BusinessException(ErrorCode.BANK_ACCOUNT_NOT_FOUND);
        }
        return debtor.getCreditor().getId();
    }

    private BankAccountResponse toResponse(BankAccount account) {
        return new BankAccountResponse(account.getBankName(), account.getAccountNo(),
                account.getAccountName(), account.getPaymentNote());
    }
}
