package com.leets.tdd.user.controller;

import com.leets.tdd.global.common.ApiResponse;
import com.leets.tdd.user.domain.DormSemesterCalculator;
import com.leets.tdd.user.dto.DormSemesterEndResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 운영/관리자 보조용 내부 도구. 기숙사 인증(FR-AUTH-02) 승인은 MVP 범위상 API 없이 관리자가
 * DB를 직접 UPDATE하는 방식으로 확정되어 있다({@code docs/operations/dormitory-verification-admin-guide.md}
 * 참고). 그 수동 작업 중 dorm_verified_until 값을 손으로 계산하다 실수하는 걸 막기 위한 계산기다.
 * 데이터를 조회/변경하지 않는 순수 계산이라 인증 없이 열어둔다(SecurityConfig의 PUBLIC_ENDPOINTS 참고).
 */
@Tag(name = "Internal", description = "운영/관리자 보조용 내부 도구 API(인증 불필요, 데이터 조회·변경 없음)")
@RestController
@RequestMapping("/api/v1/internal")
public class InternalToolController {

    @Operation(
            summary = "기숙사 인증 학기 만료일 계산",
            description = "date(YYYY-MM-DD, 인증 시각 기준)를 넣으면 그 인증이 속한 학기가 끝나는 시각을 "
                    + "돌려준다. date를 생략하면 오늘 날짜 기준으로 계산한다. 관리자가 dormitory 테이블을 "
                    + "직접 UPDATE할 때 dorm_verified_until 값을 참고하는 용도다."
    )
    @GetMapping("/dorm-semester-end")
    public ResponseEntity<ApiResponse<DormSemesterEndResponse>> calcDormSemesterEnd(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDateTime verifiedAt = (date != null ? date : LocalDate.now()).atStartOfDay();
        LocalDateTime semesterEnd = DormSemesterCalculator.calcSemesterEnd(verifiedAt);
        return ResponseEntity.ok(
                ApiResponse.success("계산되었습니다.", new DormSemesterEndResponse(semesterEnd)));
    }
}
