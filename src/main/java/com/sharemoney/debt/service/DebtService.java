package com.sharemoney.debt.service;

import com.sharemoney.admin.service.InstallmentChoiceService;
import com.sharemoney.common.domain.UserRole;
import com.sharemoney.common.error.BusinessException;
import com.sharemoney.common.error.ErrorCode;
import com.sharemoney.common.security.SecurityUtils;
import com.sharemoney.debt.dto.CreateDebtRequest;
import com.sharemoney.debt.dto.DebtDetailResponse;
import com.sharemoney.debt.dto.DebtSummaryResponse;
import com.sharemoney.debt.dto.FullPaidRequest;
import com.sharemoney.debt.dto.ReorderRequest;
import com.sharemoney.debt.entity.Debt;
import com.sharemoney.debt.entity.DebtMethod;
import com.sharemoney.debt.entity.DebtStatus;
import com.sharemoney.debt.entity.Installment;
import com.sharemoney.debt.entity.InstallmentKind;
import com.sharemoney.debt.entity.OpenLoanRecord;
import com.sharemoney.debt.entity.PaymentStatus;
import com.sharemoney.debt.repository.DebtRepository;
import com.sharemoney.debt.repository.InstallmentRepository;
import com.sharemoney.debt.repository.OpenLoanRecordRepository;
import com.sharemoney.user.entity.User;
import com.sharemoney.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DebtService {

    private final DebtRepository debtRepository;
    private final InstallmentRepository installmentRepository;
    private final OpenLoanRecordRepository openLoanRecordRepository;
    private final UserRepository userRepository;
    private final InstallmentChoiceService installmentChoiceService;
    private final DebtAccessService debtAccessService;

    @Transactional(readOnly = true)
    public List<DebtSummaryResponse> list(String debtorUsername, String search) {
        var current = SecurityUtils.currentUser();
        String pattern = toSearchPattern(search);
        String debtorFilter = StringUtils.hasText(debtorUsername) ? debtorUsername.toLowerCase() : null;

        List<Debt> debts = current.getRole() == UserRole.DEBTOR
                ? debtRepository.findAllForDebtor(current.getId(), pattern)
                : debtRepository.findAllForCreditor(current.getId(), debtorFilter, pattern);

        if (debts.isEmpty()) {
            return List.of();
        }

        List<Long> debtIds = debts.stream().map(Debt::getId).toList();

        Map<Long, List<Installment>> installmentsByDebtId = installmentRepository.findByDebt_IdIn(debtIds).stream()
                .collect(Collectors.groupingBy(installment -> installment.getDebt().getId()));
        Map<Long, List<OpenLoanRecord>> openRecordsByDebtId = openLoanRecordRepository.findByDebt_IdIn(debtIds).stream()
                .collect(Collectors.groupingBy(record -> record.getDebt().getId()));

        return debts.stream()
                .map(debt -> DebtCalculator.toSummary(debt,
                        installmentsByDebtId.getOrDefault(debt.getId(), List.of()),
                        openRecordsByDebtId.getOrDefault(debt.getId(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public DebtDetailResponse getDetail(Long debtId) {
        return DebtCalculator.toDetail(debtAccessService.loadViewable(debtId));
    }

    @Transactional
    public DebtDetailResponse create(CreateDebtRequest request) {
        User creditor = userRepository.findById(SecurityUtils.currentUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        User debtor = userRepository.findByUsernameIgnoreCase(request.debtorUsername())
                .filter(user -> user.getRole() == UserRole.DEBTOR)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        boolean ownedByCreditor = debtor.getCreditor() != null && debtor.getCreditor().getId().equals(creditor.getId());
        if (!ownedByCreditor) {
            throw new BusinessException(ErrorCode.DEBTOR_NOT_OWNED);
        }

        Debt debt = switch (request.method()) {
            case INSTALLMENT -> buildInstallmentDebt(request, creditor, debtor);
            case OPEN -> buildOpenDebt(request, creditor, debtor);
            case FULL -> throw new BusinessException(ErrorCode.INVALID_DEBT_METHOD,
                    "FULL debts can only be created through legacy data migration.");
        };

        debtRepository.save(debt);
        return DebtCalculator.toDetail(debt);
    }

    @Transactional
    public void delete(Long debtId) {
        Debt debt = debtAccessService.loadOwnedByCreditor(debtId);
        debtRepository.delete(debt);
    }

    @Transactional
    public void reorder(ReorderRequest request) {
        Long creditorId = SecurityUtils.currentUserId();
        List<Debt> debts = debtRepository.findAllById(request.orderedIds());

        for (Debt debt : debts) {
            if (!debt.getCreditor().getId().equals(creditorId)) {
                throw new BusinessException(ErrorCode.DEBT_NOT_OWNED);
            }
        }

        Map<Long, Debt> debtsById = debts.stream().collect(Collectors.toMap(Debt::getId, debt -> debt));
        List<Long> orderedIds = request.orderedIds();
        for (int i = 0; i < orderedIds.size(); i++) {
            Debt debt = debtsById.get(orderedIds.get(i));
            if (debt != null) {
                debt.setSortOrder(i);
            }
        }
    }

    @Transactional
    public DebtDetailResponse setFullPaid(Long debtId, FullPaidRequest request) {
        Debt debt = debtAccessService.loadOwnedByCreditor(debtId);
        if (debt.getMethod() != DebtMethod.FULL) {
            throw new BusinessException(ErrorCode.INVALID_DEBT_METHOD);
        }

        if (Boolean.TRUE.equals(request.paid())) {
            debt.setStatus(DebtStatus.PAID);
            debt.setPaidAmount(debt.getPrincipalAmount());
            debt.setPaidAt(Instant.now());
        } else {
            debt.setStatus(DebtStatus.PENDING);
            debt.setPaidAmount(BigDecimal.ZERO);
            debt.setPaidAt(null);
        }

        return DebtCalculator.toDetail(debt);
    }

    private Debt buildInstallmentDebt(CreateDebtRequest request, User creditor, User debtor) {
        if (request.installmentCount() == null || request.installmentAmount() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "installmentCount and installmentAmount are required for INSTALLMENT method.");
        }
        installmentChoiceService.assertValidChoice(request.installmentCount());

        Debt debt = new Debt();
        debt.setCreditor(creditor);
        debt.setDebtor(debtor);
        debt.setTitle(request.title());
        debt.setDescription(request.description());
        debt.setMethod(DebtMethod.INSTALLMENT);
        debt.setStartDate(request.startDate());
        debt.setInstallmentCount(request.installmentCount());
        debt.setInstallmentAmount(request.installmentAmount());
        debt.setPrincipalAmount(request.installmentAmount().multiply(BigDecimal.valueOf(request.installmentCount())));
        debt.setStatus(DebtStatus.PENDING);
        debt.setPaidAmount(BigDecimal.ZERO);

        for (int no = 1; no <= request.installmentCount(); no++) {
            Installment installment = new Installment();
            installment.setDebt(debt);
            installment.setNo(no);
            installment.setAmount(request.installmentAmount());
            installment.setKind(InstallmentKind.PRINCIPAL);
            installment.setStatus(PaymentStatus.UNPAID);
            installment.setDueDate(request.startDate().plusMonths(no - 1L));
            debt.getInstallments().add(installment);
        }
        return debt;
    }

    private Debt buildOpenDebt(CreateDebtRequest request, User creditor, User debtor) {
        if (request.principal() == null || request.installmentAmount() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "principal and installmentAmount (initial monthly interest) are required for OPEN method.");
        }

        Debt debt = new Debt();
        debt.setCreditor(creditor);
        debt.setDebtor(debtor);
        debt.setTitle(request.title());
        debt.setDescription(request.description());
        debt.setMethod(DebtMethod.OPEN);
        debt.setStartDate(request.startDate());
        debt.setInstallmentAmount(request.installmentAmount());
        debt.setPrincipalAmount(request.principal());
        debt.setStatus(DebtStatus.PENDING);
        debt.setPaidAmount(BigDecimal.ZERO);

        OpenLoanRecord record = new OpenLoanRecord();
        record.setDebt(debt);
        record.setNo(1);
        record.setPayDate(request.startDate());
        record.setTotalPaid(BigDecimal.ZERO);
        record.setInterest(request.installmentAmount());
        record.setRemainingPrincipal(request.principal());
        record.setStatus(PaymentStatus.UNPAID);
        debt.getOpenLoanRecords().add(record);

        return debt;
    }

    private String toSearchPattern(String search) {
        return StringUtils.hasText(search) ? "%" + search.toLowerCase() + "%" : null;
    }
}
