UPDATE delivery_parties
SET status = 'DELIVERED'
WHERE status = 'COMPLETED';

UPDATE delivery_parties
SET status = 'SETTLED'
WHERE status = 'DELIVERED'
  AND settlement_status = 'COMPLETED';
