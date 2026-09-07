package com.sharemoney.debt.service;

import com.sharemoney.common.error.BusinessException;
import com.sharemoney.common.error.ErrorCode;
import com.sharemoney.debt.dto.DebtDetailResponse;
import com.sharemoney.debt.dto.OpenRecordCreateRequest;
import com.sharemoney.debt.dto.OpenRecordUpdateRequest;
import com.sharemoney.debt.entity.Debt;
import com.sharemoney.debt.entity.DebtMethod;
import com.sharemoney.debt.entity.OpenLoanRecord;
import com.sharemoney.debt.entity.PaymentStatus;
import com.sharemoney.debt.repository.OpenLoanRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OpenLoanRecordService {

    private final DebtAccessService debtAccessService;
    private final OpenLoanRecordRepository openLoanRecordRepository;

    @Transactional
    public DebtDetailResponse add(Long debtId, OpenRecordCreateRequest request) {
        Debt debt = loadOpenDebt(debtId);

        int nextNo = debt.getOpenLoanRecords().stream()
                .mapToInt(OpenLoanRecord::getNo)
                .max()
                .orElse(0) + 1;

        OpenLoanRecord record = new OpenLoanRecord();
        record.setDebt(debt);
        record.setNo(nextNo);
        record.setPayDate(request.payDate());
        record.setInterest(request.interest());
        record.setRemainingPrincipal(request.remainingPrincipal());
        record.setTotalPaid(BigDecimal.ZERO);
        record.setStatus(Boolean.TRUE.equals(request.paid()) ? PaymentStatus.PAID : PaymentStatus.UNPAID);
        record.setPaidAt(Boolean.TRUE.equals(request.paid()) ? Instant.now() : null);
        debt.getOpenLoanRecords().add(record);

        DebtCalculator.recalculateOpenDebt(debt);
        return DebtCalculator.toDetail(debt);
    }

    @Transactional
    public DebtDetailResponse update(Long debtId, int no, OpenRecordUpdateRequest request) {
        Debt debt = loadOpenDebt(debtId);
        OpenLoanRecord record = findRecord(debt, no);

        record.setTotalPaid(request.totalPaid());
        record.setStatus(request.status());
        record.setPaidAt(request.status() == PaymentStatus.PAID ? Instant.now() : null);

        DebtCalculator.recalculateOpenDebt(debt);
        return DebtCalculator.toDetail(debt);
    }

    @Transactional
    public DebtDetailResponse delete(Long debtId, int no) {
        Debt debt = loadOpenDebt(debtId);
        OpenLoanRecord toRemove = findRecord(debt, no);

        debt.getOpenLoanRecords().remove(toRemove);
        openLoanRecordRepository.delete(toRemove);
        openLoanRecordRepository.flush();

        List<OpenLoanRecord> remaining = debt.getOpenLoanRecords().stream()
                .sorted(Comparator.comparingInt(OpenLoanRecord::getNo))
                .toList();
        for (int i = 0; i < remaining.size(); i++) {
            remaining.get(i).setNo(i + 1);
        }

        DebtCalculator.recalculateOpenDebt(debt);
        return DebtCalculator.toDetail(debt);
    }

    private Debt loadOpenDebt(Long debtId) {
        Debt debt = debtAccessService.loadOwnedByCreditor(debtId);
        if (debt.getMethod() != DebtMethod.OPEN) {
            throw new BusinessException(ErrorCode.INVALID_DEBT_METHOD);
        }
        return debt;
    }

    private OpenLoanRecord findRecord(Debt debt, int no) {
        return debt.getOpenLoanRecords().stream()
                .filter(record -> record.getNo() == no)
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.OPEN_RECORD_NOT_FOUND));
    }
}
