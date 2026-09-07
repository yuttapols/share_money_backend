package com.sharemoney.admin.service;

import com.sharemoney.admin.entity.Setting;
import com.sharemoney.admin.repository.SettingRepository;
import com.sharemoney.common.error.BusinessException;
import com.sharemoney.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InstallmentChoiceService {

    private static final String SETTING_KEY = "installment_choices";

    private final SettingRepository settingRepository;

    @Transactional(readOnly = true)
    public List<Integer> getChoices() {
        return settingRepository.findById(SETTING_KEY)
                .map(setting -> parse(setting.getValue()))
                .orElse(List.of());
    }

    @Transactional(readOnly = true)
    public void assertValidChoice(int count) {
        if (!getChoices().contains(count)) {
            throw new BusinessException(ErrorCode.INVALID_INSTALLMENT_COUNT);
        }
    }

    @Transactional
    public List<Integer> addChoice(int count) {
        Setting setting = getSetting();
        List<Integer> choices = new ArrayList<>(parse(setting.getValue()));
        if (!choices.contains(count)) {
            choices.add(count);
            choices.sort(Integer::compareTo);
            setting.setValue(join(choices));
        }
        return choices;
    }

    @Transactional
    public List<Integer> removeChoice(int count) {
        Setting setting = getSetting();
        List<Integer> choices = new ArrayList<>(parse(setting.getValue()));
        choices.remove(Integer.valueOf(count));
        setting.setValue(join(choices));
        return choices;
    }

    private Setting getSetting() {
        return settingRepository.findById(SETTING_KEY)
                .orElseThrow(() -> new BusinessException(ErrorCode.SETTING_NOT_FOUND));
    }

    private List<Integer> parse(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(Integer::parseInt)
                .toList();
    }

    private String join(List<Integer> choices) {
        return choices.stream().map(String::valueOf).collect(Collectors.joining(","));
    }
}
