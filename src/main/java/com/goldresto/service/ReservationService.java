package com.goldresto.service;

import com.goldresto.entity.Produit;
import com.goldresto.entity.Reservation;
import com.goldresto.entity.User;
import com.goldresto.repository.ProduitRepository;
import com.goldresto.repository.ReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ReservationService {
    
    private static final Logger logger = LoggerFactory.getLogger(ReservationService.class);
    
    @Autowired
    private ReservationRepository reservationRepository;
    
    @Autowired
    private ProduitRepository produitRepository;
    
    @Transactional
    public Reservation createReservation(Long produitId, Integer quantite, 
                                       LocalDate dateReservation, User user, String notes) {
        logger.info("Creating reservation for produit {} quantity {} date {}", produitId, quantite, dateReservation);
        
        Produit produit = produitRepository.findById(produitId)
            .orElseThrow(() -> new IllegalArgumentException("Produit non trouvé: " + produitId));
            
        // Vérifier si le stock disponible est suffisant
        Integer reserveActuel = reservationRepository.sumQuantiteByProduitAndDate(produitId, dateReservation);
        Integer stockDisponible = produit.getStock() != null ? produit.getStock().intValue() : 0;
        
        if (stockDisponible < quantite) {
            throw new IllegalStateException("Stock insuffisant pour réserver. Disponible: " + stockDisponible + ", Demandé: " + quantite);
        }
        
        Reservation reservation = new Reservation();
        reservation.setProduit(produit);
        reservation.setUser(user);
        reservation.setQuantite(quantite);
        reservation.setDateReservation(dateReservation);
        reservation.setDateLimite(dateReservation.minusDays(1)); // J-1 pour annuler
        reservation.setNotes(notes);
        reservation.setStatus(Reservation.ReservationStatus.EN_ATTENTE);
        
        return reservationRepository.save(reservation);
    }
    
    @Transactional
    public Reservation confirmReservation(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("Réservation non trouvée: " + reservationId));
            
        if (reservation.getStatus() != Reservation.ReservationStatus.EN_ATTENTE) {
            throw new IllegalStateException("Seule une réservation en attente peut être confirmée");
        }
        
        reservation.setStatus(Reservation.ReservationStatus.CONFIRMEE);
        logger.info("Reservation {} confirmed", reservationId);
        return reservationRepository.save(reservation);
    }
    
    @Transactional
    public Reservation cancelReservation(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("Réservation non trouvée: " + reservationId));
            
        if (reservation.getStatus() == Reservation.ReservationStatus.ANNULEE) {
            throw new IllegalStateException("Réservation déjà annulée");
        }
        
        reservation.setStatus(Reservation.ReservationStatus.ANNULEE);
        logger.info("Reservation {} cancelled", reservationId);
        return reservationRepository.save(reservation);
    }
    
    @Transactional
    public Reservation completeReservation(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("Réservation non trouvée: " + reservationId));
            
        if (reservation.getStatus() != Reservation.ReservationStatus.CONFIRMEE) {
            throw new IllegalStateException("Seule une réservation confirmée peut être terminée");
        }
        
        reservation.setStatus(Reservation.ReservationStatus.TERMINEE);
        logger.info("Reservation {} completed", reservationId);
        return reservationRepository.save(reservation);
    }
    
    public List<Reservation> getReservationsByDate(LocalDate date) {
        return reservationRepository.findByDateReservation(date);
    }
    
    public List<Reservation> getReservationsByUser(User user) {
        return reservationRepository.findByUser(user);
    }
    
    public Integer getReservedQuantity(Long produitId, LocalDate date) {
        return reservationRepository.sumQuantiteByProduitAndDate(produitId, date);
    }
    
    public Integer getAvailableStock(Long produitId, LocalDate date) {
        Produit produit = produitRepository.findById(produitId)
            .orElseThrow(() -> new IllegalArgumentException("Produit non trouvé: " + produitId));
        
        Integer reserved = getReservedQuantity(produitId, date);
        Integer stock = produit.getStock() != null ? produit.getStock().intValue() : 0;
        return stock - reserved;
    }
}
