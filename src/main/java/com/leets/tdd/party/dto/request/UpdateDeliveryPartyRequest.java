package com.leets.tdd.party.dto.request;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateDeliveryPartyRequest {

    private String title;

    private String description;

    private Integer maxParticipants;

    private LocalDateTime orderExpectedAt;


    public UpdateDeliveryPartyRequest(
            String title,
            String description,
            Integer maxParticipants,
            LocalDateTime orderExpectedAt
    ) {
        this.title = title;
        this.description = description;
        this.maxParticipants = maxParticipants;
        this.orderExpectedAt = orderExpectedAt;
    }
}
