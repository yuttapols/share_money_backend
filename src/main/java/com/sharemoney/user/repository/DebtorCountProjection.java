package com.sharemoney.user.repository;

public interface DebtorCountProjection {

    Long getCreditorId();

    long getDebtorCount();
}
