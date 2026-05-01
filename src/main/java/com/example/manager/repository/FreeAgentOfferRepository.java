package com.example.manager.repository;

import com.example.manager.model.FreeAgentOffer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FreeAgentOfferRepository extends JpaRepository<FreeAgentOffer, Long> {

	/**
	 * Findet alle Angebote eines Teams
	 */
	List<FreeAgentOffer> findByTeamId(Long teamId);

	/**
	 * Findet alle Angebote für einen Spieler
	 */
	List<FreeAgentOffer> findByPlayerId(Long playerId);

	/**
	 * Findet ein spezifisches Angebot eines Teams für einen Spieler
	 */
	Optional<FreeAgentOffer> findByPlayerIdAndTeamId(Long playerId, Long teamId);

	/**
	 * Löscht alle Angebote für einen Spieler (wenn er unterschrieben hat)
	 */
	void deleteByPlayerId(Long playerId);
}
