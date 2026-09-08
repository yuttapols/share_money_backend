package com.sharemoney.profile.service;

import com.sharemoney.common.domain.UserRole;
import com.sharemoney.common.error.BusinessException;
import com.sharemoney.common.error.ErrorCode;
import com.sharemoney.common.security.SecurityUtils;
import com.sharemoney.profile.dto.BankAccountResponse;
import com.sharemoney.profile.dto.CreateBankAccountRequest;
import com.sharemoney.profile.dto.UpdateBankAccountRequest;
import com.sharemoney.profile.entity.BankAccount;
import com.sharemoney.profile.repository.BankAccountRepository;
import com.sharemoney.user.entity.User;
import com.sharemoney.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BankAccountService {

    private final BankAccountRepository bankAccountRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<BankAccountResponse> list() {
        Long creditorId = resolveCreditorId();
        if (creditorId == null) {
            return List.of();
        }
        return bankAccountRepository.findAllByCreditor_IdOrderByIdAsc(creditorId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public BankAccountResponse create(CreateBankAccountRequest request) {
        User creditor = userRepository.findById(SecurityUtils.currentUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        BankAccount account = new BankAccount();
        account.setCreditor(creditor);
        account.setBankName(request.bankName());
        account.setAccountNo(request.accountNo());
        account.setAccountName(request.accountName());
        account.setPaymentNote(request.paymentNote());
        bankAccountRepository.save(account);

        return toResponse(account);
    }

    @Transactional
    public BankAccountResponse update(Long id, UpdateBankAccountRequest request) {
        BankAccount account = loadOwned(id);

        account.setBankName(request.bankName());
        account.setAccountNo(request.accountNo());
        account.setAccountName(request.accountName());
        account.setPaymentNote(request.paymentNote());
        account.setUpdatedAt(Instant.now());

        return toResponse(account);
    }

    @Transactional
    public void delete(Long id) {
        BankAccount account = loadOwned(id);
        bankAccountRepository.delete(account);
    }

    private BankAccount loadOwned(Long id) {
        BankAccount account = bankAccountRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.BANK_ACCOUNT_NOT_FOUND));
        if (!account.getCreditor().getId().equals(SecurityUtils.currentUserId())) {
            throw new BusinessException(ErrorCode.BANK_ACCOUNT_NOT_OWNED);
        }
        return account;
    }

    private Long resolveCreditorId() {
        if (SecurityUtils.currentRole() != UserRole.DEBTOR) {
            return SecurityUtils.currentUserId();
        }
        User debtor = userRepository.findById(SecurityUtils.currentUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return debtor.getCreditor() == null ? null : debtor.getCreditor().getId();
    }

    private BankAccountResponse toResponse(BankAccount account) {
        return new BankAccountResponse(account.getId(), account.getBankName(), account.getAccountNo(),
                account.getAccountName(), account.getPaymentNote());
    }
}
