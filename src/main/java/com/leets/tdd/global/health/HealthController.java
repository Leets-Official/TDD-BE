package com.leets.tdd.global.health;

import com.leets.tdd.global.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @GetMapping
    public ApiResponse<HealthStatus> health() {
        return ApiResponse.success("서버가 정상 동작 중입니다.", new HealthStatus("UP"));
    }

    public record HealthStatus(String status) {
    }
}
