package com.benly.session.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CompanyType {

    BIG_TECH_SW("대기업 SW", "삼성·SK하이닉스·LG CNS"),
    SERVICE("서비스 기업", "네이버·카카오·쿠팡·배민"),
    FINANCE_IT("금융 IT", "토스·카카오뱅크·증권/은행 IT");

    private final String label;
    private final String example;
}
