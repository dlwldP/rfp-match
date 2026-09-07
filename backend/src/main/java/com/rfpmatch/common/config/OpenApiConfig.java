package com.rfpmatch.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI rfpMatchOpenApi() {
        return new OpenAPI().info(new Info()
                .title("보안입찰 RFP-스펙 매칭 API")
                .version("v0.1.0")
                .description("""
                        공공 입찰공고를 수집하고, RFP 요구사항과 자사 제품 스펙을 대조해
                        충족/부분충족/미충족 갭분석표를 만들어 주는 API.
                        """));
    }
}
