CREATE INDEX idx_reports_pending_duplicate_lookup
    ON reports (party_id, reporter_id, reported_user_id, reason, status);
