package com.sharemoney.user.service;

import com.sharemoney.auth.repository.RefreshTokenRepository;
import com.sharemoney.common.domain.UserRole;
import com.sharemoney.common.error.BusinessException;
import com.sharemoney.common.error.ErrorCode;
import com.sharemoney.common.security.SecurityUtils;
import com.sharemoney.common.storage.TransactionalFileCleanup;
import com.sharemoney.debt.repository.DebtRepository;
import com.sharemoney.document.entity.Document;
import com.sharemoney.document.repository.DocumentRepository;
import com.sharemoney.slip.entity.Slip;
import com.sharemoney.slip.repository.SlipRepository;
import com.sharemoney.user.dto.CreateDebtorRequest;
import com.sharemoney.user.dto.DebtorResponse;
import com.sharemoney.user.dto.DeleteDebtorResponse;
import com.sharemoney.user.dto.UpdateDebtorRequest;
import com.sharemoney.user.entity.User;
import com.sharemoney.user.mapper.UserMapper;
import com.sharemoney.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DebtorService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final SlipRepository slipRepository;
    private final DocumentRepository documentRepository;
    private final DebtRepository debtRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TransactionalFileCleanup fileCleanup;

    @Transactional
    public DebtorResponse create(CreateDebtorRequest request) {
        if (userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new BusinessException(ErrorCode.USERNAME_DUPLICATE);
        }

        User creditor = userRepository.findById(SecurityUtils.currentUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        User debtor = new User();
        debtor.setUsername(request.username());
        debtor.setPasswordHash(passwordEncoder.encode(request.password()));
        debtor.setFullName(request.name());
        debtor.setRole(UserRole.DEBTOR);
        debtor.setCreditor(creditor);
        debtor.setActive(true);
        userRepository.save(debtor);

        return userMapper.toDebtorResponse(debtor);
    }

    @Transactional(readOnly = true)
    public List<DebtorResponse> list(String search) {
        String searchPattern = toSearchPattern(search);
        List<User> debtors = SecurityUtils.isAdmin()
                ? userRepository.searchAllDebtors(UserRole.DEBTOR, searchPattern)
                : userRepository.searchDebtorsByCreditor(UserRole.DEBTOR, SecurityUtils.currentUserId(), searchPattern);
        return debtors.stream().map(userMapper::toDebtorResponse).toList();
    }

    private String toSearchPattern(String search) {
        return StringUtils.hasText(search) ? "%" + search.toLowerCase() + "%" : null;
    }

    @Transactional
    public DebtorResponse update(String username, UpdateDebtorRequest request) {
        User debtor = getOwnedDebtor(username);

        if (!debtor.getUsername().equalsIgnoreCase(request.username())
                && userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new BusinessException(ErrorCode.USERNAME_DUPLICATE);
        }

        debtor.setUsername(request.username());
        debtor.setFullName(request.name());

        if (StringUtils.hasText(request.password())) {
            if (request.password().length() < 6) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "password: size must be between 6 and 100");
            }
            debtor.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        return userMapper.toDebtorResponse(debtor);
    }

    @Transactional
    public DeleteDebtorResponse delete(String username) {
        User debtor = getOwnedDebtor(username);
        Long debtorId = debtor.getId();

        List<Slip> slips = slipRepository.findByDebtor_Id(debtorId);
        slips.forEach(slip -> fileCleanup.deleteAfterCommit(slip.getPublicId(), slip.getResourceType(), true));
        slipRepository.deleteAllInBatch(slips);

        List<Document> documents = documentRepository.findByDebtor_Id(debtorId);
        documents.forEach(doc -> fileCleanup.deleteAfterCommit(doc.getPublicId(), doc.getResourceType(), true));
        documentRepository.deleteAllInBatch(documents);

        long deletedDebts = debtRepository.deleteByDebtorId(debtorId);
        refreshTokenRepository.deleteByUserId(debtorId);

        userRepository.delete(debtor);
        return new DeleteDebtorResponse(true, deletedDebts);
    }

    private User getOwnedDebtor(String username) {
        User debtor = userRepository.findByUsernameIgnoreCase(username)
                .filter(user -> user.getRole() == UserRole.DEBTOR)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        boolean ownedByCurrentCreditor = debtor.getCreditor() != null
                && debtor.getCreditor().getId().equals(SecurityUtils.currentUserId());

        if (!SecurityUtils.isAdmin() && !ownedByCurrentCreditor) {
            throw new BusinessException(ErrorCode.DEBTOR_NOT_OWNED);
        }
        return debtor;
    }
}
