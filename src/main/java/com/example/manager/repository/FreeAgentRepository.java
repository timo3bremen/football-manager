package com.example.manager.repository;

import com.example.manager.model.FreeAgent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FreeAgentRepository extends JpaRepository<FreeAgent, Long> {

	/**
	 * Findet einen freien Spieler anhand der PlayerId
	 */
	Optional<FreeAgent> findByPlayerId(Long playerId);

	/**
	 * Findet alle verfügbaren freien Spieler
	 */
	List<FreeAgent> findByStatus(String status);

	/**
	 * Findet alle freien Spieler deren Deadline abgelaufen ist
	 */
	List<FreeAgent> findByDecisionDeadlineBeforeAndStatus(LocalDateTime deadline, String status);

	/**
	 * Löscht einen freien Spieler anhand der PlayerId
	 */
	void deleteByPlayerId(Long playerId);
}
