package com.sharemoney.user.mapper;

import com.sharemoney.user.dto.CreditorResponse;
import com.sharemoney.user.dto.DebtorResponse;
import com.sharemoney.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "name", source = "fullName")
    CreditorResponse toCreditorResponse(User user);

    @Mapping(target = "name", source = "fullName")
    @Mapping(target = "creditorUsername", source = "creditor.username")
    DebtorResponse toDebtorResponse(User user);
}
