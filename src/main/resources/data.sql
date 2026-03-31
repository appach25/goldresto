-- Ajouter la colonne date_anniversaire si elle n'existe pas
ALTER TABLE clients ADD COLUMN IF NOT EXISTS date_anniversaire DATE;
