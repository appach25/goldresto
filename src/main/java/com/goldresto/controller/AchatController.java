package com.goldresto.controller;

import com.goldresto.entity.Achat;
import com.goldresto.entity.Ingredient;
import com.goldresto.entity.LigneAchat;
import com.goldresto.repository.AchatRepository;
import com.goldresto.repository.IngredientRepository;
import com.goldresto.repository.LigneAchatRepository;
import com.goldresto.service.IngredientStockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequestMapping("/achats")
@PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
public class AchatController {
    @Autowired
    private AchatRepository achatRepository;

    @Autowired
    private IngredientRepository ingredientRepository;

    @Autowired
    private LigneAchatRepository ligneAchatRepository;

    @Autowired
    private IngredientStockService ingredientStockService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("achats", achatRepository.findAll());
        return "achats/list";
    }

    @GetMapping("/new")
    public String newAchat(Model model) {
        Achat achat = new Achat();
        achat.setDateAchat(LocalDate.now());
        model.addAttribute("achat", achat);
        return "achats/form";
    }

    @PostMapping
    public String createAchat(@ModelAttribute Achat achat) {
        if (achat.getDateAchat() == null) {
            achat.setDateAchat(LocalDate.now());
        }
        Achat saved = achatRepository.save(achat);
        return "redirect:/achats/" + saved.getId();
    }

    @GetMapping("/{id}")
    public String viewAchat(@PathVariable Long id, Model model) {
        Achat achat = achatRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Invalid achat Id:" + id));
        model.addAttribute("achat", achat);
        model.addAttribute("lignes", achat.getLignes());
        model.addAttribute("ingredients", ingredientRepository.findAll());
        model.addAttribute("ligneAchat", new LigneAchat());
        return "achats/detail";
    }

    @PostMapping("/{id}/lignes")
    public String addLigne(
        @PathVariable Long id,
        @RequestParam Long ingredientId,
        @RequestParam BigDecimal quantite,
        @RequestParam BigDecimal prixUnitaire
    ) {
        Achat achat = achatRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Invalid achat Id:" + id));

        LigneAchat ligne = new LigneAchat();
        ligne.setAchat(achat);
        Ingredient ingredient = ingredientRepository.findById(ingredientId)
            .orElseThrow(() -> new IllegalArgumentException("Invalid ingredient Id:" + ingredientId));
        ligne.setIngredient(ingredient);
        ligne.setQuantite(quantite);
        ligne.setUnite(ingredient.getUniteStock());
        ligne.setPrixUnitaire(prixUnitaire);

        achat.getLignes().add(ligne);
        ligneAchatRepository.save(ligne);
        ingredientStockService.increaseStockOnAchat(ingredient.getId(), quantite);
        achat.updateTotal();
        achatRepository.save(achat);
        return "redirect:/achats/" + id;
    }

    @GetMapping("/{id}/lignes/{ligneId}/delete")
    public String deleteLigne(@PathVariable Long id, @PathVariable Long ligneId) {
        LigneAchat ligne = ligneAchatRepository.findById(ligneId)
            .orElseThrow(() -> new IllegalArgumentException("Invalid ligne Id:" + ligneId));
        Achat achat = ligne.getAchat();
        ligneAchatRepository.delete(ligne);
        achat.updateTotal();
        achatRepository.save(achat);
        return "redirect:/achats/" + id;
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Achat achat = achatRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Invalid achat Id:" + id));
        model.addAttribute("achat", achat);
        return "achats/edit";
    }

    @PostMapping("/{id}")
    public String updateAchat(@PathVariable Long id, @ModelAttribute Achat achat) {
        Achat existing = achatRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Invalid achat Id:" + id));
        existing.setFournisseur(achat.getFournisseur());
        existing.setDateAchat(achat.getDateAchat());
        existing.setReferenceFacture(achat.getReferenceFacture());
        achatRepository.save(existing);
        return "redirect:/achats/" + id;
    }

    @GetMapping("/{id}/delete")
    public String deleteAchat(@PathVariable Long id) {
        achatRepository.deleteById(id);
        return "redirect:/achats";
    }
}
