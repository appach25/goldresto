package com.goldresto.service;

import com.goldresto.entity.Client;
import com.goldresto.repository.ClientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class ClientService {
    
    private static final Logger logger = LoggerFactory.getLogger(ClientService.class);
    
    private final ClientRepository clientRepository;
    
    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }
    
    // CRUD Operations
    
    @Transactional(readOnly = true)
    public List<Client> getAllClients() {
        logger.debug("Récupération de tous les clients");
        return clientRepository.findAll();
    }
    
    @Transactional(readOnly = true)
    public Page<Client> getAllClients(Pageable pageable) {
        Page<Client> clients = clientRepository.findAll(pageable);
        return clients;
    }
    
    @Transactional(readOnly = true)
    public Client getClientById(Long id) {
        logger.debug("Récupération du client avec ID: {}", id);
        return clientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Client non trouvé avec l'ID: " + id));
    }
    
    public Client createClient(Client client) {
        client.setCreatedAt(LocalDateTime.now());
        client.setUpdatedAt(LocalDateTime.now());
        client.setActif(true);
        
        // Validation de l'unicité de l'email
        if (client.getEmail() != null && !client.getEmail().trim().isEmpty()) {
            Optional<Client> existingClient = clientRepository.findByEmail(client.getEmail());
            if (existingClient.isPresent()) {
                throw new IllegalArgumentException("Un client avec cet email existe déjà: " + client.getEmail());
            }
        }
        
        // Validation de l'unicité du téléphone
        if (client.getTelephone() != null && !client.getTelephone().trim().isEmpty()) {
            Optional<Client> existingPhone = clientRepository.findByTelephone(client.getTelephone());
            if (existingPhone.isPresent()) {
                throw new IllegalArgumentException("Un client avec ce numéro de téléphone existe déjà: " + client.getTelephone());
            }
        }
        
        Client savedClient = clientRepository.save(client);
        return savedClient;
    }
    
    public Client updateClient(Long id, Client clientDetails) {
        logger.info("Mise à jour du client ID: {}", id);
        
        Client existingClient = getClientById(id);
        
        // Validation de l'unicité de l'email (si modifié)
        if (clientDetails.getEmail() != null && !clientDetails.getEmail().trim().isEmpty()) {
            if (!clientDetails.getEmail().equals(existingClient.getEmail())) {
                if (clientRepository.existsByEmailAndIdNot(clientDetails.getEmail(), id)) {
                    throw new IllegalArgumentException("Un autre client avec cet email existe déjà: " + clientDetails.getEmail());
                }
            }
        }
        
        // Validation de l'unicité du téléphone (si modifié)
        if (clientDetails.getTelephone() != null && !clientDetails.getTelephone().trim().isEmpty()) {
            if (!clientDetails.getTelephone().equals(existingClient.getTelephone())) {
                if (clientRepository.existsByTelephoneAndIdNot(clientDetails.getTelephone(), id)) {
                    throw new IllegalArgumentException("Un autre client avec ce numéro de téléphone existe déjà: " + clientDetails.getTelephone());
                }
            }
        }
        
        // Mise à jour des champs
        existingClient.setNom(clientDetails.getNom());
        existingClient.setPrenom(clientDetails.getPrenom());
        existingClient.setEmail(clientDetails.getEmail());
        existingClient.setTelephone(clientDetails.getTelephone());
        existingClient.setAdresse(clientDetails.getAdresse());
        existingClient.setVille(clientDetails.getVille());
        existingClient.setCodePostal(clientDetails.getCodePostal());
        existingClient.setNotes(clientDetails.getNotes());
        existingClient.setActif(clientDetails.getActif());
        
        Client updatedClient = clientRepository.save(existingClient);
        logger.info("Client mis à jour avec succès: ID {}", updatedClient.getId());
        return updatedClient;
    }
    
    public void deleteClient(Long id) {
        logger.info("Suppression du client ID: {}", id);
        
        Client client = getClientById(id);
        
        // Vérifier si le client a des réservations
        if (client.hasReservations()) {
            // On désactive au lieu de supprimer pour garder l'historique
            client.setActif(false);
            clientRepository.save(client);
            logger.warn("Client désactivé (avec réservations) au lieu d'être supprimé: ID {}", id);
        } else {
            clientRepository.delete(client);
            logger.info("Client supprimé avec succès: ID {}", id);
        }
    }
    
    // Recherche et filtrage
    
    @Transactional(readOnly = true)
    public Page<Client> searchClients(String searchTerm, Pageable pageable) {
        logger.debug("Recherche de clients avec terme: '{}'", searchTerm);
        
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return clientRepository.findAll(pageable);
        }
        
        return clientRepository.findBySearchTerm(searchTerm.trim(), pageable);
    }
    
    @Transactional(readOnly = true)
    public List<Client> getActiveClients() {
        logger.debug("Récupération des clients actifs");
        return clientRepository.findByActifTrue();
    }
    
    @Transactional(readOnly = true)
    public List<Client> getInactiveClients() {
        logger.debug("Récupération des clients inactifs");
        return clientRepository.findByActifFalse();
    }
    
    @Transactional(readOnly = true)
    public List<Client> getClientsWithReservations() {
        logger.debug("Récupération des clients avec réservations");
        return clientRepository.findClientsWithReservations();
    }
    
    @Transactional(readOnly = true)
    public List<Client> getClientsWithoutReservations() {
        logger.debug("Récupération des clients sans réservations");
        return clientRepository.findClientsWithoutReservations();
    }
    
    // Méthodes utilitaires
    
    @Transactional(readOnly = true)
    public Optional<Client> findByEmail(String email) {
        logger.debug("Recherche de client par email: {}", email);
        return clientRepository.findByEmail(email);
    }
    
    @Transactional(readOnly = true)
    public Optional<Client> findByTelephone(String telephone) {
        logger.debug("Recherche de client par téléphone: {}", telephone);
        return clientRepository.findByTelephone(telephone);
    }
    
    @Transactional(readOnly = true)
    public List<Client> getNewClientsSince(LocalDateTime date) {
        logger.debug("Récupération des nouveaux clients depuis: {}", date);
        return clientRepository.findByCreatedAtAfter(date);
    }
    
    @Transactional(readOnly = true)
    public List<Client> getInactiveClientsSince(LocalDateTime date) {
        logger.debug("Récupération des clients inactifs depuis: {}", date);
        return clientRepository.findInactiveClientsSince(date);
    }
    
    @Transactional(readOnly = true)
    public List<Client> getMostLoyalClients(Pageable pageable) {
        logger.debug("Récupération des clients les plus fidèles");
        return clientRepository.findMostLoyalClients(pageable);
    }
    
    // Statistiques
    
    @Transactional(readOnly = true)
    public Map<String, Object> getClientStatistics() {
        logger.debug("Calcul des statistiques des clients");
        
        long totalClients = clientRepository.countTotalClients();
        long activeClients = clientRepository.countActiveClients();
        long newClientsThisMonth = clientRepository.countNewClientsSince(LocalDateTime.now().minusMonths(1));
        long inactiveClients = totalClients - activeClients;
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalClients", totalClients);
        stats.put("activeClients", activeClients);
        stats.put("inactiveClients", inactiveClients);
        stats.put("newClientsThisMonth", newClientsThisMonth);
        stats.put("clientsWithReservations", clientRepository.findClientsWithReservations().size());
        stats.put("clientsWithoutReservations", clientRepository.findClientsWithoutReservations().size());
        
        return stats;
    }
    
    // Validation et vérification
    
    @Transactional(readOnly = true)
    public boolean emailExists(String email) {
        return clientRepository.findByEmail(email).isPresent();
    }
    
    @Transactional(readOnly = true)
    public boolean telephoneExists(String telephone) {
        return clientRepository.findByTelephone(telephone).isPresent();
    }
    
    @Transactional(readOnly = true)
    public boolean emailExistsForOtherClient(String email, Long excludeId) {
        return clientRepository.existsByEmailAndIdNot(email, excludeId);
    }
    
    @Transactional(readOnly = true)
    public boolean telephoneExistsForOtherClient(String telephone, Long excludeId) {
        return clientRepository.existsByTelephoneAndIdNot(telephone, excludeId);
    }
    
    // Activation/Désactivation
    
    public void activateClient(Long id) {
        logger.info("Activation du client ID: {}", id);
        Client client = getClientById(id);
        client.setActif(true);
        clientRepository.save(client);
    }
    
    public void deactivateClient(Long id) {
        logger.info("Désactivation du client ID: {}", id);
        Client client = getClientById(id);
        client.setActif(false);
        clientRepository.save(client);
    }
}
