package com.leets.tdd.party.repository.projection;

import java.time.LocalDateTime;

public interface MyDeliveryPartyProjection {
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
