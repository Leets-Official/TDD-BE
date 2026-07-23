ALTER TABLE delivery_parties
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE party_participants
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

CREATE INDEX idx_delivery_parties_creator_settlement_status
    ON delivery_parties (creator_id, settlement_status);

CREATE INDEX idx_party_participants_party_status
    ON party_participants (party_id, status);

CREATE INDEX idx_party_participants_user_payment_status
    ON party_participants (user_id, payment_status);
