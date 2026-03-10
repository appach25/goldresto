package com.goldresto.repository;

import com.goldresto.entity.LigneAchat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LigneAchatRepository extends JpaRepository<LigneAchat, Long> {
}
