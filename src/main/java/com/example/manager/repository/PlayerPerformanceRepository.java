package com.example.manager.repository;

import com.example.manager.model.PlayerPerformance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlayerPerformanceRepository extends JpaRepository<PlayerPerformance, Long> {

	/**
	 * Findet alle Performances eines Spielers, sortiert nach MatchId (neueste zuerst)
	 */
	@Query("SELECT pp FROM PlayerPerformance pp WHERE pp.playerId = :playerId ORDER BY pp.matchId DESC")
	List<PlayerPerformance> findByPlayerIdOrderByMatchIdDesc(@Param("playerId") Long playerId);

	/**
	 * Findet die letzten N Performances eines Spielers
	 */
	@Query(value = "SELECT * FROM player_performances WHERE player_id = :playerId ORDER BY match_id DESC LIMIT :limit", nativeQuery = true)
	List<PlayerPerformance> findLastNPerformancesByPlayerId(@Param("playerId") Long playerId, @Param("limit") int limit);

	/**
	 * Findet alle Performances eines Spielers in einem bestimmten Spiel
	 */
	List<PlayerPerformance> findByMatchIdAndPlayerId(Long matchId, Long playerId);

	/**
	 * Findet alle Performances in einem Spiel
	 */
	List<PlayerPerformance> findByMatchId(Long matchId);
}
