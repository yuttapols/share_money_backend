package com.sharemoney.admin.dto;

import java.util.List;

public record InstallmentChoicesResponse(
        List<Integer> choices
) {
}
