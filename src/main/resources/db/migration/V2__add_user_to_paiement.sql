-- Add user_id column to paiement table
ALTER TABLE paiement ADD COLUMN user_id BIGINT;

-- Update existing payments to set user_id from panier
UPDATE paiement p 
SET user_id = (
    SELECT user_id 
    FROM panier 
    WHERE id = p.panier_id
)
WHERE p.user_id IS NULL;

-- Add foreign key constraint and index after data migration
ALTER TABLE paiement ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE paiement ADD CONSTRAINT fk_paiement_user FOREIGN KEY (user_id) REFERENCES users(id);
CREATE INDEX idx_paiement_user ON paiement(user_id);
