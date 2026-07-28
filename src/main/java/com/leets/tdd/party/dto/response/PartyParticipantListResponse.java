package com.leets.tdd.party.dto.response;

import java.util.List;

public record PartyParticipantListResponse(
        Long partyId,
        List<PartyParticipantResponse> participants
) {
}
