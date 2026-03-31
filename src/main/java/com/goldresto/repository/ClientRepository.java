package com.goldresto.repository;

import com.goldresto.entity.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
    
    // Recherche par email unique
    Optional<Client> findByEmail(String email);
    
    // Recherche par nom ou prénom
    List<Client> findByNomContainingIgnoreCase(String nom);
    List<Client> findByPrenomContainingIgnoreCase(String prenom);
    
    // Recherche combinée nom/prénom
    @Query("SELECT c FROM Client c WHERE " +
           "LOWER(c.nom) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.prenom) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.telephone) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Client> findBySearchTerm(@Param("search") String search, Pageable pageable);
    
    // Clients actifs/inactifs
    List<Client> findByActifTrue();
    List<Client> findByActifFalse();
    
    // Clients avec réservations
    @Query("SELECT c FROM Client c JOIN c.reservations r WHERE r.id IS NOT NULL")
    List<Client> findClientsWithReservations();
    
    // Clients sans réservations
    @Query("SELECT c FROM Client c LEFT JOIN c.reservations r WHERE r.id IS NULL")
    List<Client> findClientsWithoutReservations();
    
    // Clients récemment créés
    List<Client> findByCreatedAtAfter(LocalDateTime date);
    
    // Clients inactifs (sans réservation depuis une date)
    @Query("SELECT c FROM Client c WHERE " +
           "c NOT IN (SELECT DISTINCT r.client FROM Reservation r WHERE r.dateReservation >= :date)")
    List<Client> findInactiveClientsSince(@Param("date") LocalDateTime date);
    
    // Statistiques
    @Query("SELECT COUNT(c) FROM Client c")
    long countTotalClients();
    
    @Query("SELECT COUNT(c) FROM Client c WHERE c.actif = true")
    long countActiveClients();
    
    @Query("SELECT COUNT(c) FROM Client c WHERE c.createdAt >= :date")
    long countNewClientsSince(@Param("date") LocalDateTime date);
    
    // Clients les plus fidèles (avec le plus de réservations)
    @Query("SELECT c FROM Client c " +
           "JOIN c.reservations r " +
           "GROUP BY c " +
           "ORDER BY COUNT(r) DESC")
    List<Client> findMostLoyalClients(Pageable pageable);
    
    // Recherche par téléphone
    Optional<Client> findByTelephone(String telephone);
    
    // Vérification email existe (pour un autre client)
    boolean existsByEmailAndIdNot(String email, Long id);
    
    // Vérification téléphone existe (pour un autre client)
    boolean existsByTelephoneAndIdNot(String telephone, Long id);
}
