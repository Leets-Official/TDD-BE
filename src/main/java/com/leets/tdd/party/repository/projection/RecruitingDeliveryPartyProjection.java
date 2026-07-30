package com.leets.tdd.party.repository.projection;

import java.time.LocalDateTime;

public interface RecruitingDeliveryPartyProjection {
    Long getPartyId();
    String getTitle();
    String getCategory();
    Long getCurrentParticipants();
    Integer getMinParticipants();
    Integer getMaxParticipants();
    String getStatus();
    LocalDateTime getOrderExpectedAt();
    String getDormitory();
}
