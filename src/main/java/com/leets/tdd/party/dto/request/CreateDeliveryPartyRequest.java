package com.leets.tdd.party.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class CreateDeliveryPartyRequest {

    private Long foodCategoryId;

    @NotBlank(message = "기숙사를 선택해주세요.")
    @Pattern(regexp = "^(1기숙사|2기숙사|3기숙사)$", message = "기숙사 동을 선택해주세요.")
    private String dormitory;

    private String title;

    private String description;

    private Integer minParticipants;

    private Integer maxParticipants;

    private LocalDateTime orderExpectedAt;
}
