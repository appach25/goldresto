package com.goldresto.controller;

import com.goldresto.entity.Produit;
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
}
