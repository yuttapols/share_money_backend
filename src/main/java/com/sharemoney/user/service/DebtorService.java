package com.sharemoney.user.service;

import com.sharemoney.common.domain.UserRole;
import com.sharemoney.common.error.BusinessException;
import com.sharemoney.common.error.ErrorCode;
import com.sharemoney.common.security.SecurityUtils;
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
        List<User> debtors = SecurityUtils.isAdmin()
                ? userRepository.searchAllDebtors(UserRole.DEBTOR, search)
                : userRepository.searchDebtorsByCreditor(UserRole.DEBTOR, SecurityUtils.currentUserId(), search);
        return debtors.stream().map(userMapper::toDebtorResponse).toList();
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
        userRepository.delete(debtor);
        return new DeleteDebtorResponse(true, 0L);
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
