package com.example.manager.repository;

import com.example.manager.model.Scout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScoutRepository extends JpaRepository<Scout, Long> {
    List<Scout> findByTeamId(Long teamId);
    Optional<Scout> findByTeamIdAndIsActive(Long teamId, boolean isActive);
}
