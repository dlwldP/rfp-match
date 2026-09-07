package com.rfpmatch.common.exception;

/** 요청한 리소스(공고/제품/요구사항)가 없을 때. 404로 매핑된다. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }

    public static NotFoundException bid(Long id) {
        return new NotFoundException("입찰공고를 찾을 수 없습니다. bidId=" + id);
    }

    public static NotFoundException product(Long id) {
        return new NotFoundException("제품을 찾을 수 없습니다. productId=" + id);
    }

    public static NotFoundException requirement(Long id) {
        return new NotFoundException("요구사항 항목을 찾을 수 없습니다. requirementId=" + id);
    }
}
