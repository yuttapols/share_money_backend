package com.sharemoney.debt.service;

import com.sharemoney.common.domain.UserRole;
import com.sharemoney.common.error.BusinessException;
import com.sharemoney.common.error.ErrorCode;
import com.sharemoney.common.security.SecurityUtils;
import com.sharemoney.common.security.UserPrincipal;
import com.sharemoney.debt.entity.Debt;
import com.sharemoney.debt.repository.DebtRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DebtAccessService {

    private final DebtRepository debtRepository;

    public Debt loadOwnedByCreditor(Long debtId) {
        Debt debt = debtRepository.findDetailById(debtId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEBT_NOT_FOUND));

        Long currentUserId = SecurityUtils.currentUserId();
        if (!debt.getCreditor().getId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.DEBT_NOT_OWNED);
        }
        return debt;
    }

    public Debt loadViewable(Long debtId) {
        Debt debt = debtRepository.findDetailById(debtId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEBT_NOT_FOUND));

        UserPrincipal current = SecurityUtils.currentUser();
        boolean viewableByCreditor = current.getRole() == UserRole.CREDITOR && debt.getCreditor().getId().equals(current.getId());
        boolean viewableByDebtor = current.getRole() == UserRole.DEBTOR && debt.getDebtor().getId().equals(current.getId());

        if (!viewableByCreditor && !viewableByDebtor) {
            throw new BusinessException(ErrorCode.DEBT_NOT_OWNED);
        }
        return debt;
    }
}
