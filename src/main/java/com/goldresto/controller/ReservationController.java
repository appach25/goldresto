package com.goldresto.controller;

import com.goldresto.entity.Produit;
import com.goldresto.entity.Reservation;
import com.goldresto.entity.User;
import com.goldresto.repository.ProduitRepository;
import com.goldresto.repository.ReservationRepository;
import com.goldresto.service.ReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/reservations")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'EMPLOYEE')")
public class ReservationController {
    
    private static final Logger logger = LoggerFactory.getLogger(ReservationController.class);
    
    @Autowired
    private ReservationService reservationService;
    
    @Autowired
    private ReservationRepository reservationRepository;
    
    @Autowired
    private ProduitRepository produitRepository;
    
    @GetMapping
    public String listReservations(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model) {
        
        LocalDate selectedDate = date != null ? date : LocalDate.now();
        List<Reservation> reservations = reservationService.getReservationsByDate(selectedDate);
        
        model.addAttribute("reservations", reservations);
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("today", LocalDate.now());
        
        return "reservations/list";
    }
    
    @GetMapping("/calendar")
    public String calendarView(Model model) {
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());
        
        List<Reservation> monthReservations = reservationRepository.findByDateRange(startOfMonth, endOfMonth);
        model.addAttribute("reservations", monthReservations);
        model.addAttribute("currentDate", today);
        
        return "reservations/calendar";
    }
    
    @GetMapping("/new")
    public String createReservationForm(Model model) {
        model.addAttribute("reservation", new Reservation());
        model.addAttribute("produits", produitRepository.findAll());
        model.addAttribute("today", LocalDate.now());
        return "reservations/form";
    }
    
    @PostMapping
    public String saveReservation(@ModelAttribute Reservation reservation,
                                 @RequestParam Long produitId,
                                 Principal principal,
                                 RedirectAttributes redirectAttributes) {
        try {
            User user = null; // TODO: Get current user from security context
            reservation = reservationService.createReservation(
                produitId, 
                reservation.getQuantite(), 
                reservation.getDateReservation(), 
                user, 
                reservation.getNotes()
            );
            
            redirectAttributes.addFlashAttribute("success", "Réservation créée avec succès");
            return "redirect:/reservations?date=" + reservation.getDateReservation();
            
        } catch (Exception e) {
            logger.error("Error creating reservation", e);
            redirectAttributes.addFlashAttribute("error", "Erreur: " + e.getMessage());
            return "redirect:/reservations/new";
        }
    }
    
    @PostMapping("/{id}/confirm")
    public String confirmReservation(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            reservationService.confirmReservation(id);
            redirectAttributes.addFlashAttribute("success", "Réservation confirmée");
        } catch (Exception e) {
            logger.error("Error confirming reservation", e);
            redirectAttributes.addFlashAttribute("error", "Erreur: " + e.getMessage());
        }
        return "redirect:/reservations";
    }
    
    @PostMapping("/{id}/cancel")
    public String cancelReservation(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            reservationService.cancelReservation(id);
            redirectAttributes.addFlashAttribute("success", "Réservation annulée");
        } catch (Exception e) {
            logger.error("Error cancelling reservation", e);
            redirectAttributes.addFlashAttribute("error", "Erreur: " + e.getMessage());
        }
        return "redirect:/reservations";
    }
    
    @PostMapping("/{id}/complete")
    public String completeReservation(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            reservationService.completeReservation(id);
            redirectAttributes.addFlashAttribute("success", "Réservation terminée");
        } catch (Exception e) {
            logger.error("Error completing reservation", e);
            redirectAttributes.addFlashAttribute("error", "Erreur: " + e.getMessage());
        }
        return "redirect:/reservations";
    }
    
    @GetMapping("/check-availability")
    @ResponseBody
    public String checkAvailability(@RequestParam Long produitId, 
                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                   @RequestParam Integer quantity) {
        try {
            Integer available = reservationService.getAvailableStock(produitId, date);
            return available >= quantity ? "AVAILABLE" : "INSUFFICIENT:" + available;
        } catch (Exception e) {
            return "ERROR:" + e.getMessage();
        }
    }
}
