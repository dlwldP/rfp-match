package com.rfpmatch.bid.domain;

/** 공고 진행 상태. 마감일시(closeDate) 기준으로 계산된다. */
public enum BidStatus {
    OPEN,
    CLOSED,
    UNKNOWN
}
