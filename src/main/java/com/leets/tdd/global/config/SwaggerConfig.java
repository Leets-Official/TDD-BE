package com.leets.tdd.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI 우측 상단에 "Authorize" 버튼(자물쇠 아이콘)을 띄우기 위한 설정.
 * @AuthenticationPrincipal로 바꾸면서 컨트롤러 파라미터에 Authorization 헤더가 더 이상
 * 안 보이므로, 이 버튼에 access token을 한 번 입력해두면 이후 모든 요청에 자동으로
 * "Authorization: Bearer <token>" 헤더가 붙는다. 실제로 어떤 엔드포인트에 인증이 필요한지는
 * 각 컨트롤러 메서드의 @SecurityRequirement(name = "bearerAuth")로 표시한다.
 */
@Configuration
public class SwaggerConfig {

    private static final String BEARER_AUTH_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        SecurityScheme bearerAuthScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");

        return new OpenAPI()
                .components(new Components().addSecuritySchemes(BEARER_AUTH_SCHEME_NAME, bearerAuthScheme));
    }
}
