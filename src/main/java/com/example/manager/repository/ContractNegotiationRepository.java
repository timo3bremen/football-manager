package com.example.manager.repository;

import com.example.manager.model.ContractNegotiation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ContractNegotiationRepository extends JpaRepository<ContractNegotiation, Long> {

	/**
	 * Findet die Verhandlungshistorie für einen Spieler in einer bestimmten Saison
	 */
	Optional<ContractNegotiation> findByPlayerIdAndSeason(Long playerId, int season);

	/**
	 * Löscht alle Verhandlungen eines Spielers (wenn Vertrag erfolgreich verlängert)
	 */
	void deleteByPlayerId(Long playerId);
}
