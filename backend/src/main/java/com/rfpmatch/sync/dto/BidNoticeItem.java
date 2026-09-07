package com.rfpmatch.sync.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 입찰공고 Open API 응답 1건에서 이 시스템이 쓰는 필드만 추린 것.
 * 오퍼레이션마다 필드가 더 많이 내려오므로 모르는 필드는 무시한다.
 *
 * @param bidNtceNo     공고번호
 * @param bidNtceOrd    공고차수
 * @param bidNtceNm     공고명
 * @param ntceInsttNm   공고기관명
 * @param dminsttNm     수요기관명
 * @param bidClseDt     입찰마감일시
 * @param presmptPrce   추정가격
 * @param bidNtceDtlUrl 공고 상세 URL
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BidNoticeItem(
        String bidNtceNo,
        String bidNtceOrd,
        String bidNtceNm,
        String ntceInsttNm,
        String dminsttNm,
        String bidClseDt,
        String presmptPrce,
        String bidNtceDtlUrl) {
}
