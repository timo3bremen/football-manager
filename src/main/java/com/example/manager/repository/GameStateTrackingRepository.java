package com.example.manager.repository;

import com.example.manager.model.GameStateTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for GameStateTracking.
 */
@Repository
public interface GameStateTrackingRepository extends JpaRepository<GameStateTracking, Long> {
}
