package com.rfpmatch.common.config;

import com.rfpmatch.sync.client.BidNoticeApiProperties;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

/** 외부 API가 응답하지 않을 때 스케줄러 스레드가 묶이지 않도록 타임아웃을 건다. */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClientCustomizer timeoutCustomizer(BidNoticeApiProperties properties) {
        int timeoutMillis = (int) properties.timeout().toMillis();
        return builder -> {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(timeoutMillis);
            factory.setReadTimeout(timeoutMillis);
            builder.requestFactory(factory);
        };
    }
}
