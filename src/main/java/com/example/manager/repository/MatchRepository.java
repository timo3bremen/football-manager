package com.example.manager.repository;

import com.example.manager.model.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for Match entities.
 */
@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {
}
