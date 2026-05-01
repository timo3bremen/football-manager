package com.example.manager.model;

import jakarta.persistence.*;

/**
 * Speichert die Verhandlungshistorie eines Spielers
 * Wird zurückgesetzt wenn Vertrag erfolgreich verlängert wurde
 */
@Entity
@Table(name = "contract_negotiations")
public class ContractNegotiation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "playerId")
	private Long playerId;

	@Column(name = "teamId")
	private Long teamId;

	// Anzahl der Versuche in dieser Saison (max 3)
	private int attemptCount = 0;

	// Saison in der verhandelt wurde
	private int season;

	// Ist die Verhandlung gescheitert? (alle 3 Versuche aufgebraucht)
	private boolean failed = false;

	public ContractNegotiation() {
	}

	public ContractNegotiation(Long playerId, Long teamId, int season) {
		this.playerId = playerId;
		this.teamId = teamId;
		this.season = season;
		this.attemptCount = 0;
		this.failed = false;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getPlayerId() {
		return playerId;
	}

	public void setPlayerId(Long playerId) {
		this.playerId = playerId;
	}

	public Long getTeamId() {
		return teamId;
	}

	public void setTeamId(Long teamId) {
		this.teamId = teamId;
	}

	public int getAttemptCount() {
		return attemptCount;
	}

	public void setAttemptCount(int attemptCount) {
		this.attemptCount = attemptCount;
	}

	public int getSeason() {
		return season;
	}

	public void setSeason(int season) {
		this.season = season;
	}

	public boolean isFailed() {
		return failed;
	}

	public void setFailed(boolean failed) {
		this.failed = failed;
	}

	public void incrementAttempt() {
		this.attemptCount++;
		if (this.attemptCount >= 3) {
			this.failed = true;
		}
	}
}
