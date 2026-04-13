package com.example.manager.repository;

import com.example.manager.model.Matchday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for Matchday entities.
 */
@Repository
public interface MatchdayRepository extends JpaRepository<Matchday, Long> {
}
