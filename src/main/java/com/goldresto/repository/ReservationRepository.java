package com.goldresto.repository;

import com.goldresto.entity.Client;
import com.goldresto.entity.Produit;
import com.goldresto.entity.Reservation;
import com.goldresto.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    
    List<Reservation> findByDateReservation(LocalDate date);
    
    List<Reservation> findByStatus(Reservation.ReservationStatus status);
    
    List<Reservation> findByUser(User user);
    
    List<Reservation> findByClient(Client client);
    
    List<Reservation> findByProduit(Produit produit);
    
    @Query("SELECT COALESCE(SUM(r.quantite), 0) FROM Reservation r " +
           "WHERE r.produit.id = :produitId AND r.dateReservation = :date " +
           "AND r.status != 'ANNULEE'")
    Integer sumQuantiteByProduitAndDate(@Param("produitId") Long produitId, @Param("date") LocalDate date);
    
    @Query("SELECT r FROM Reservation r WHERE r.dateReservation >= :startDate AND r.dateReservation <= :endDate")
    List<Reservation> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
