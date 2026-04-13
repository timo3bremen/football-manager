package com.example.manager.repository;

import com.example.manager.model.League;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for League entities.
 */
@Repository
public interface LeagueRepository extends JpaRepository<League, Long> {
}
