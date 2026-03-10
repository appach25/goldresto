package com.goldresto.controller;

import com.goldresto.entity.Ingredient;
import com.goldresto.entity.Produit;
import com.goldresto.entity.ProduitIngredient;
import com.goldresto.repository.IngredientRepository;
import com.goldresto.repository.ProduitIngredientRepository;
import com.goldresto.repository.ProduitRepository;
import com.goldresto.service.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/produits")
@PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
public class ProduitController {

    @Autowired
    private ProduitRepository produitRepository;

    @Autowired
    private IngredientRepository ingredientRepository;

    @Autowired
    private ProduitIngredientRepository produitIngredientRepository;

    @Autowired
    private StorageService storageService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("produits", produitRepository.findAll());
        return "produits/list";
    }

    @GetMapping("/gallery")
    public String gallery(Model model) {
        model.addAttribute("produits", produitRepository.findAll());
        return "produits/gallery";
    }

    @GetMapping("/new")
    public String newProduitForm(Model model) {
        model.addAttribute("produit", new Produit());
        return "produits/form";
    }

    @GetMapping("/edit/{id}")
    public String editProduitForm(@PathVariable Long id, Model model) {
        Produit produit = produitRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid produit Id:" + id));
        model.addAttribute("produit", produit);
        return "produits/form";
    }

    @PostMapping("/save")
    public String saveProduit(@ModelAttribute Produit produit, @RequestParam(required = false) MultipartFile file) throws IOException {
        if (file != null && !file.isEmpty()) {
            String fileName = storageService.saveFile(file);
            produit.setImagePath("uploads/images/" + fileName);
        }
        
        produitRepository.save(produit);
        return "redirect:/produits";
    }

    @GetMapping("/delete/{id}")
    public String deleteProduit(@PathVariable Long id) {
        Produit produit = produitRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid produit Id:" + id));
        produitRepository.delete(produit);
        return "redirect:/produits";
    }

    @GetMapping("/{id}/ingredients")
    public String viewIngredients(@PathVariable Long id, Model model) {
        Produit produit = produitRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid produit Id:" + id));
        model.addAttribute("produit", produit);
        model.addAttribute("ingredients", produitIngredientRepository.findByProduitId(id));
        model.addAttribute("catalog", ingredientRepository.findAll());
        model.addAttribute("ingredient", new Ingredient());
        return "produits/ingredients";
    }

    @PostMapping("/{id}/ingredients")
    public String addIngredient(
        @PathVariable Long id,
        @RequestParam(required = false) Long ingredientId,
        @RequestParam(required = false) String nom,
        @RequestParam(required = false) java.math.BigDecimal quantite
    ) {
        Produit produit = produitRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid produit Id:" + id));
        if (produit.isNoRecipe()) {
            return "redirect:/produits/" + id + "/ingredients";
        }
        Ingredient ingredient;
        if (ingredientId != null) {
            ingredient = ingredientRepository.findById(ingredientId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid ingredient Id:" + ingredientId));
        } else {
            if (nom == null || nom.isBlank()) {
                return "redirect:/produits/" + id + "/ingredients";
            }
            ingredient = ingredientRepository.findByNomIgnoreCase(nom)
                .orElseGet(() -> {
                    Ingredient created = new Ingredient();
                    created.setNom(nom.trim());
                    return ingredientRepository.save(created);
                });
        }

        ProduitIngredient produitIngredient = new ProduitIngredient();
        produitIngredient.setProduit(produit);
        produitIngredient.setIngredient(ingredient);
        produitIngredient.setQuantite(quantite);
        produitIngredient.setUnite(ingredient.getUniteStock());
        produitIngredientRepository.save(produitIngredient);
        return "redirect:/produits/" + id + "/ingredients";
    }

    @GetMapping("/{id}/ingredients/{ingredientId}/delete")
    public String deleteIngredient(@PathVariable Long id, @PathVariable Long ingredientId) {
        produitIngredientRepository.deleteById(ingredientId);
        return "redirect:/produits/" + id + "/ingredients";
    }
}
