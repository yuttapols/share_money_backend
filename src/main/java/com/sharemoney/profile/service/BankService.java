package com.sharemoney.profile.service;

import com.sharemoney.profile.dto.BankResponse;
import com.sharemoney.profile.repository.BankRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BankService {

    private final BankRepository bankRepository;

    @Transactional(readOnly = true)
    public List<BankResponse> list() {
        return bankRepository.findAllByOrderBySortOrderAsc().stream()
                .map(bank -> new BankResponse(bank.getCode(), bank.getName()))
                .toList();
    }
}
