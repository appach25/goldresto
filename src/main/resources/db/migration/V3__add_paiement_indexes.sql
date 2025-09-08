-- Add composite index for user payments by date
CREATE INDEX idx_paiement_user_date ON paiement(user_id, date_creation);

-- Add index for date range queries
CREATE INDEX idx_paiement_date ON paiement(date_creation);

-- Add index for panier state
CREATE INDEX idx_panier_state ON panier(state);
