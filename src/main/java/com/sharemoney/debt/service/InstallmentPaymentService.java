package com.sharemoney.debt.service;

import com.sharemoney.common.error.BusinessException;
import com.sharemoney.common.error.ErrorCode;
import com.sharemoney.common.period.BillingPeriod;
import com.sharemoney.debt.dto.DebtDetailResponse;
import com.sharemoney.debt.dto.PayInstallmentRequest;
import com.sharemoney.debt.dto.PayInterestRequest;
import com.sharemoney.debt.entity.Debt;
import com.sharemoney.debt.entity.DebtMethod;
import com.sharemoney.debt.entity.Installment;
import com.sharemoney.debt.entity.InstallmentInterestPayment;
import com.sharemoney.debt.entity.InstallmentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

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
            installment.setStatus(InstallmentStatus.PAID);
            installment.setPayDate(request.payDate());
            installment.setPaidAt(Instant.now());
        } else {
            installment.setStatus(unpaidStatusOf(installment));
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

        if (installment.getStatus() == InstallmentStatus.PAID) {
            throw new BusinessException(ErrorCode.INSTALLMENT_ALREADY_PAID);
        }
        Installment nextDue = DebtCalculator.nextDueInstallment(debt.getInstallments());
        if (nextDue != installment) {
            throw new BusinessException(ErrorCode.INSTALLMENT_NOT_NEXT_DUE);
        }
        if (installment.hasInterestPaidIn(BillingPeriod.of(request.payDate()))) {
            throw new BusinessException(ErrorCode.INTEREST_ALREADY_PAID_THIS_PERIOD);
        }

        InstallmentInterestPayment payment = new InstallmentInterestPayment();
        payment.setInstallment(installment);
        payment.setPayDate(request.payDate());
        payment.setAmount(request.amount() != null ? request.amount() : installment.getAmount());
        payment.setPaidAt(Instant.now());
        installment.getInterestPayments().add(payment);
        installment.setStatus(InstallmentStatus.INTEREST_PAID);

        DebtCalculator.recalculateInstallmentDebt(debt);
        return DebtCalculator.toDetail(debt);
    }

    @Transactional
    public DebtDetailResponse cancelInterest(Long debtId, int no) {
        Debt debt = loadInstallmentDebt(debtId);
        Installment installment = findInstallment(debt, no);

        if (installment.getStatus() == InstallmentStatus.PAID) {
            throw new BusinessException(ErrorCode.INSTALLMENT_ALREADY_PAID);
        }
        List<InstallmentInterestPayment> payments = installment.getInterestPayments();
        if (payments.isEmpty()) {
            throw new BusinessException(ErrorCode.INTEREST_PAYMENT_NOT_FOUND);
        }

        payments.remove(payments.size() - 1);
        installment.setStatus(unpaidStatusOf(installment));

        DebtCalculator.recalculateInstallmentDebt(debt);
        return DebtCalculator.toDetail(debt);
    }

    private InstallmentStatus unpaidStatusOf(Installment installment) {
        return installment.getInterestPayments().isEmpty() ? InstallmentStatus.UNPAID : InstallmentStatus.INTEREST_PAID;
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
