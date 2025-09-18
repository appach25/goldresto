-- Add promotion fields to produit table if they don't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name='produit' AND column_name='promo_qty'
    ) THEN
        ALTER TABLE produit ADD COLUMN promo_qty integer;
    END IF;

    IF NOT EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name='produit' AND column_name='promo_price'
    ) THEN
        ALTER TABLE produit ADD COLUMN promo_price numeric(19,2);
    END IF;
END $$;
