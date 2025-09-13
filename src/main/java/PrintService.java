@Service
public class PrintService {
    // ...existing code...

    public void printProduitAjoute(Produit produit, Long commandeId) {
        if (produit == null || commandeId == null) {
            throw new IllegalArgumentException("Produit or commandeId is null");
        }
        // ...printing logic...
    }

    // ...existing code...
}