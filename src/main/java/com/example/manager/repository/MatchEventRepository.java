package com.example.manager.repository;

import com.example.manager.model.MatchEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for MatchEvent entities.
 */
@Repository
public interface MatchEventRepository extends JpaRepository<MatchEvent, Long> {
	List<MatchEvent> findByMatchId(Long matchId);
	
	List<MatchEvent> findByPlayerId(Long playerId);
	
	void deleteByMatchId(Long matchId);
}
