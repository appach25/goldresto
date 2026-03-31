package com.goldresto.repository;

import com.goldresto.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    
    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r.name IN :roles")
    List<User> findByRolesNameIn(@Param("roles") List<String> roles);
}
