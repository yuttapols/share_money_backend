package com.sharemoney.debt.service;

import com.sharemoney.common.error.BusinessException;
import com.sharemoney.common.error.ErrorCode;
import com.sharemoney.debt.dto.DebtDetailResponse;
import com.sharemoney.debt.dto.PayInstallmentRequest;
import com.sharemoney.debt.dto.PayInterestRequest;
import com.sharemoney.debt.entity.Debt;
import com.sharemoney.debt.entity.DebtMethod;
import com.sharemoney.debt.entity.Installment;
import com.sharemoney.debt.entity.InstallmentKind;
import com.sharemoney.debt.entity.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class InstallmentPaymentService {

    private final DebtAccessService debtAccessService;

    @Transactional
    public DebtDetailResponse pay(Long debtId, int no, PayInstallmentRequest request) {
        Debt debt = loadInstallmentDebt(debtId);
        Installment installment = findInstallment(debt, no);

        if (Boolean.TRUE.equals(request.paid())) {
            if (request.payDate() == null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "payDate is required when paid=true.");
            }
            installment.setStatus(PaymentStatus.PAID);
            installment.setPayDate(request.payDate());
            installment.setPaidAt(Instant.now());
        } else {
            installment.setStatus(PaymentStatus.UNPAID);
            installment.setPayDate(null);
            installment.setPaidAt(null);
        }

        DebtCalculator.recalculateInstallmentDebt(debt);
        return DebtCalculator.toDetail(debt);
    }

    @Transactional
    public DebtDetailResponse payInterest(Long debtId, int no, PayInterestRequest request) {
        Debt debt = loadInstallmentDebt(debtId);
        Installment installment = findInstallment(debt, no);

        if (installment.getStatus() == PaymentStatus.PAID) {
            throw new BusinessException(ErrorCode.INSTALLMENT_ALREADY_PAID);
        }

        installment.setKind(InstallmentKind.INTEREST);
        installment.setStatus(PaymentStatus.PAID);
        installment.setPayDate(request.payDate());
        installment.setPaidAt(Instant.now());

        Installment last = debt.getInstallments().stream()
                .max(Comparator.comparingInt(Installment::getNo))
                .orElse(installment);

        Installment appended = new Installment();
        appended.setDebt(debt);
        appended.setNo(last.getNo() + 1);
        appended.setAmount(installment.getAmount());
        appended.setKind(InstallmentKind.PRINCIPAL);
        appended.setStatus(PaymentStatus.UNPAID);
        appended.setDueDate(last.getDueDate().plusMonths(1));
        debt.getInstallments().add(appended);

        debt.setInstallmentCount(debt.getInstallmentCount() + 1);
        debt.setPrincipalAmount(debt.getPrincipalAmount().add(installment.getAmount()));

        DebtCalculator.recalculateInstallmentDebt(debt);
        return DebtCalculator.toDetail(debt);
    }

    private Debt loadInstallmentDebt(Long debtId) {
        Debt debt = debtAccessService.loadOwnedByCreditor(debtId);
        if (debt.getMethod() != DebtMethod.INSTALLMENT) {
            throw new BusinessException(ErrorCode.INVALID_DEBT_METHOD);
        }
        return debt;
    }

    private Installment findInstallment(Debt debt, int no) {
        return debt.getInstallments().stream()
                .filter(installment -> installment.getNo() == no)
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.INSTALLMENT_NOT_FOUND));
    }
}
