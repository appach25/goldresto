-- Add index for payment user_id and date_creation
CREATE INDEX idx_payment_user_date ON paiement(user_id, date_creation);

-- Add index for payment state filtering
CREATE INDEX idx_payment_state_date ON panier(state, date);
