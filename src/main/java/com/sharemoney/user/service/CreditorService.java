package com.sharemoney.user.service;

import com.sharemoney.common.domain.UserRole;
import com.sharemoney.common.error.BusinessException;
import com.sharemoney.common.error.ErrorCode;
import com.sharemoney.user.dto.CreateCreditorRequest;
import com.sharemoney.user.dto.CreditorResponse;
import com.sharemoney.user.dto.CreditorSummaryResponse;
import com.sharemoney.user.entity.User;
import com.sharemoney.user.mapper.UserMapper;
import com.sharemoney.user.repository.DebtorCountProjection;
import com.sharemoney.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CreditorService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Transactional
    public CreditorResponse create(CreateCreditorRequest request) {
        if (userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new BusinessException(ErrorCode.USERNAME_DUPLICATE);
        }

        User creditor = new User();
        creditor.setUsername(request.username());
        creditor.setPasswordHash(passwordEncoder.encode(request.password()));
        creditor.setFullName(request.name());
        creditor.setRole(UserRole.CREDITOR);
        creditor.setActive(true);
        userRepository.save(creditor);

        return userMapper.toCreditorResponse(creditor);
    }

    @Transactional(readOnly = true)
    public List<CreditorSummaryResponse> list() {
        List<User> creditors = userRepository.findByRoleOrderByFullNameAsc(UserRole.CREDITOR);
        Map<Long, Long> debtorCounts = userRepository.countDebtorsGroupByCreditor(UserRole.DEBTOR).stream()
                .filter(projection -> projection.getCreditorId() != null)
                .collect(Collectors.toMap(DebtorCountProjection::getCreditorId, DebtorCountProjection::getDebtorCount));

        return creditors.stream()
                .map(creditor -> new CreditorSummaryResponse(
                        creditor.getId(),
                        creditor.getUsername(),
                        creditor.getFullName(),
                        creditor.isActive(),
                        debtorCounts.getOrDefault(creditor.getId(), 0L)))
                .toList();
    }
}
