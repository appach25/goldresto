package com.goldresto.controller;

import com.goldresto.entity.Ingredient;
import com.goldresto.repository.IngredientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/ingredients")
@PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
public class IngredientController {
    @Autowired
    private IngredientRepository ingredientRepository;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("ingredients", ingredientRepository.findAll());
        model.addAttribute("ingredient", new Ingredient());
        return "ingredients/list";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Ingredient ingredient = ingredientRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Invalid ingredient Id:" + id));
        model.addAttribute("ingredient", ingredient);
        return "ingredients/edit";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @ModelAttribute Ingredient ingredient) {
        Ingredient existing = ingredientRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Invalid ingredient Id:" + id));
        String nom = ingredient.getNom() != null ? ingredient.getNom().trim() : null;
        String uniteStock = ingredient.getUniteStock() != null ? ingredient.getUniteStock().trim() : null;
        if (nom == null || nom.isBlank() || uniteStock == null || uniteStock.isBlank()) {
            return "redirect:/ingredients";
        }
        boolean duplicate = ingredientRepository.findByNomIgnoreCase(nom)
            .filter(found -> !found.getId().equals(existing.getId()))
            .isPresent();
        if (duplicate) {
            return "redirect:/ingredients?error=duplicate";
        }
        existing.setNom(nom);
        existing.setUniteStock(uniteStock);
        if (ingredient.getStockActuel() != null) {
            existing.setStockActuel(ingredient.getStockActuel());
        }
        ingredientRepository.save(existing);
        return "redirect:/ingredients";
    }

    @PostMapping
    public String create(@ModelAttribute Ingredient ingredient) {
        if (ingredient.getNom() != null && !ingredient.getNom().isBlank()) {
            String nom = ingredient.getNom().trim();
            String uniteStock = ingredient.getUniteStock() != null ? ingredient.getUniteStock().trim() : null;
            ingredientRepository.findByNomIgnoreCase(nom)
                .orElseGet(() -> {
                    Ingredient created = new Ingredient();
                    created.setNom(nom);
                    created.setUniteStock(uniteStock);
                    if (ingredient.getStockActuel() != null) {
                        created.setStockActuel(ingredient.getStockActuel());
                    }
                    return ingredientRepository.save(created);
                });
        }
        return "redirect:/ingredients";
    }

    @GetMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        ingredientRepository.deleteById(id);
        return "redirect:/ingredients";
    }
}
