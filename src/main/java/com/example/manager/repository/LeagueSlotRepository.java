package com.example.manager.repository;

import com.example.manager.model.LeagueSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for LeagueSlot entities.
 */
@Repository
public interface LeagueSlotRepository extends JpaRepository<LeagueSlot, Long> {
}
