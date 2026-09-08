package com.sharemoney.slip.service;

import com.sharemoney.common.audit.AuditActions;
import com.sharemoney.common.audit.AuditLogService;
import com.sharemoney.common.domain.UserRole;
import com.sharemoney.common.error.BusinessException;
import com.sharemoney.common.error.ErrorCode;
import com.sharemoney.common.security.SecurityUtils;
import com.sharemoney.common.storage.FileStorageService;
import com.sharemoney.common.storage.FileValidator;
import com.sharemoney.common.storage.StoredFile;
import com.sharemoney.common.storage.TransactionalFileCleanup;
import com.sharemoney.slip.dto.DebtorsWithSlipResponse;
import com.sharemoney.slip.dto.SlipResponse;
import com.sharemoney.slip.entity.Slip;
import com.sharemoney.slip.repository.SlipRepository;
import com.sharemoney.user.entity.User;
import com.sharemoney.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SlipService {

    private static final long MAX_SLIP_BYTES = 1_048_576;
    private static final int MAX_SLIPS_PER_PAIR = 3;
    private static final String FOLDER = "sharemoney/slips";

    private final SlipRepository slipRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final TransactionalFileCleanup fileCleanup;
    private final AuditLogService auditLogService;

    @Transactional
    public SlipResponse upload(String creditorUsername, MultipartFile file) {
        FileValidator.assertImage(file, MAX_SLIP_BYTES);

        User debtor = userRepository.findById(SecurityUtils.currentUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        User creditor = userRepository.findByUsernameIgnoreCase(creditorUsername)
                .filter(user -> user.getRole() == UserRole.CREDITOR)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        boolean belongsToCreditor = debtor.getCreditor() != null && debtor.getCreditor().getId().equals(creditor.getId());
        if (!belongsToCreditor) {
            throw new BusinessException(ErrorCode.DEBTOR_NOT_OWNED, "You can only upload a slip to your own creditor.");
        }

        List<Slip> existing = slipRepository.findByDebtor_IdAndCreditor_IdOrderByUploadedAtDesc(debtor.getId(), creditor.getId());
        if (existing.size() >= MAX_SLIPS_PER_PAIR) {
            Slip oldest = existing.get(existing.size() - 1);
            slipRepository.delete(oldest);
            fileCleanup.deleteAfterCommit(oldest.getPublicId(), oldest.getResourceType(), true);
        }

        StoredFile stored = fileStorageService.upload(file, FOLDER, true);
        fileCleanup.deleteOnRollback(stored.publicId(), stored.resourceType(), true);

        Slip slip = new Slip();
        slip.setDebtor(debtor);
        slip.setCreditor(creditor);
        slip.setPublicId(stored.publicId());
        slip.setSecureUrl(stored.secureUrl());
        slip.setOriginalFilename(stored.originalFilename());
        slip.setFormat(stored.format());
        slip.setResourceType(stored.resourceType());
        slip.setBytes(stored.bytes());
        slipRepository.save(slip);

        auditLogService.record(debtor.getUsername(), UserRole.DEBTOR,
                AuditActions.withTarget(AuditActions.UPLOAD_SLIP, creditor.getUsername()), null);

        return toResponse(slip);
    }

    @Transactional(readOnly = true)
    public List<SlipResponse> list(String debtorUsername, String creditorUsername) {
        User debtor = userRepository.findByUsernameIgnoreCase(debtorUsername)
                .filter(user -> user.getRole() == UserRole.DEBTOR)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        User creditor = userRepository.findByUsernameIgnoreCase(creditorUsername)
                .filter(user -> user.getRole() == UserRole.CREDITOR)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        assertViewable(debtor.getId(), creditor.getId());

        return slipRepository.findByDebtor_IdAndCreditor_IdOrderByUploadedAtDesc(debtor.getId(), creditor.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DebtorsWithSlipResponse debtorsWithSlip() {
        List<String> usernames = SecurityUtils.isAdmin()
                ? slipRepository.findAllDebtorUsernamesWithSlip()
                : slipRepository.findDebtorUsernamesWithSlipByCreditor(SecurityUtils.currentUserId());
        return new DebtorsWithSlipResponse(usernames);
    }

    @Transactional
    public void delete(Long slipId) {
        Slip slip = slipRepository.findById(slipId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SLIP_NOT_FOUND));

        if (!SecurityUtils.isAdmin() && !slip.getCreditor().getId().equals(SecurityUtils.currentUserId())) {
            throw new BusinessException(ErrorCode.SLIP_NOT_OWNED);
        }

        String debtorUsername = slip.getDebtor().getUsername();
        String publicId = slip.getPublicId();
        String resourceType = slip.getResourceType();

        slipRepository.delete(slip);
        fileCleanup.deleteAfterCommit(publicId, resourceType, true);

        auditLogService.record(SecurityUtils.currentUsername(), SecurityUtils.currentRole(),
                AuditActions.withTarget(AuditActions.DELETE_SLIP, debtorUsername), null);
    }

    private void assertViewable(Long debtorId, Long creditorId) {
        if (SecurityUtils.isAdmin()) {
            return;
        }
        boolean viewableByDebtor = SecurityUtils.currentRole() == UserRole.DEBTOR && debtorId.equals(SecurityUtils.currentUserId());
        boolean viewableByCreditor = SecurityUtils.currentRole() == UserRole.CREDITOR && creditorId.equals(SecurityUtils.currentUserId());
        if (!viewableByDebtor && !viewableByCreditor) {
            throw new BusinessException(ErrorCode.SLIP_NOT_OWNED);
        }
    }

    private SlipResponse toResponse(Slip slip) {
        String url = fileStorageService.generateSignedUrl(slip.getPublicId(), slip.getResourceType());
        String thumbnailUrl = fileStorageService.generateSignedThumbnailUrl(slip.getPublicId(), slip.getResourceType(), 1200);
        return new SlipResponse(slip.getId(), slip.getDebtor().getUsername(), slip.getCreditor().getUsername(),
                slip.getOriginalFilename(), url, thumbnailUrl, slip.getBytes(), slip.getUploadedAt());
    }
}
