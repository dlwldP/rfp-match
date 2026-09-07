package com.rfpmatch.common.exception;

/**
 * 입찰공고 Open API 호출 실패. 502로 매핑되며, 스케줄러는 이 예외를 잡아 로그만 남기고
 * 마지막으로 수집한 캐시(DB)를 그대로 유지한다.
 */
public class ExternalApiException extends RuntimeException {

    public ExternalApiException(String message) {
        super(message);
    }

    public ExternalApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
