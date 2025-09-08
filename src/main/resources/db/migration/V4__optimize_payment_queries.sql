-- Add composite index for payment summary query
CREATE INDEX idx_paiement_summary ON paiement(user_id, date_creation, montantapayer);

-- Add index for panier state query
CREATE INDEX idx_panier_payment ON panier(id, state, user_id);

-- Add index for payment date range
CREATE INDEX idx_paiement_date_range ON paiement(date_creation, user_id, montantapayer);
