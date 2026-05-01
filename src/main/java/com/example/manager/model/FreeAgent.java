package com.example.manager.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Freier Spieler auf dem Transfermarkt (ohne Verein, kein Ablösegeld)
 */
@Entity
@Table(name = "free_agents")
public class FreeAgent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "playerId", unique = true)
	private Long playerId;

	// Spieltag an dem der Spieler frei wurde
	@Column(name = "matchdayCreated")
	private Integer matchdayCreated;

	// Zeitpunkt ab wann er frei ist
	@Column(name = "availableSince")
	private LocalDateTime availableSince;

	// Deadline für Entscheidung (48h nach availableSince)
	@Column(name = "decisionDeadline")
	private LocalDateTime decisionDeadline;

	// Beste Angebote (für Entscheidung)
	@Column(name = "bestOfferTeamId")
	private Long bestOfferTeamId;

	@Column(name = "bestOfferSalary")
	private Long bestOfferSalary;

	@Column(name = "bestOfferContractLength")
	private Integer bestOfferContractLength;

	// Status: "available", "offers_pending", "signed"
	private String status = "available";

	public FreeAgent() {
	}

	public FreeAgent(Long playerId, int currentMatchday) {
		this.playerId = playerId;
		this.matchdayCreated = currentMatchday;
		this.availableSince = LocalDateTime.now();
		this.decisionDeadline = LocalDateTime.now().plusHours(48);
		this.status = "available";
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

	public Integer getMatchdayCreated() {
		return matchdayCreated;
	}

	public void setMatchdayCreated(Integer matchdayCreated) {
		this.matchdayCreated = matchdayCreated;
	}

	public LocalDateTime getAvailableSince() {
		return availableSince;
	}

	public void setAvailableSince(LocalDateTime availableSince) {
		this.availableSince = availableSince;
	}

	public LocalDateTime getDecisionDeadline() {
		return decisionDeadline;
	}

	public void setDecisionDeadline(LocalDateTime decisionDeadline) {
		this.decisionDeadline = decisionDeadline;
	}

	public Long getBestOfferTeamId() {
		return bestOfferTeamId;
	}

	public void setBestOfferTeamId(Long bestOfferTeamId) {
		this.bestOfferTeamId = bestOfferTeamId;
	}

	public Long getBestOfferSalary() {
		return bestOfferSalary;
	}

	public void setBestOfferSalary(Long bestOfferSalary) {
		this.bestOfferSalary = bestOfferSalary;
	}

	public Integer getBestOfferContractLength() {
		return bestOfferContractLength;
	}

	public void setBestOfferContractLength(Integer bestOfferContractLength) {
		this.bestOfferContractLength = bestOfferContractLength;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Prüft ob ein neues Angebot besser ist als das aktuelle beste
	 * Kriterien: Gehalt (70%) + Liga-Level (30%)
	 */
	public boolean isBetterOffer(long salary, int teamDivision) {
		if (bestOfferSalary == null) {
			return true; // Erstes Angebot ist immer das beste
		}

		// Berechne Score: Gehalt-Faktor + Liga-Bonus
		double currentScore = bestOfferSalary * 0.7;
		double newScore = salary * 0.7;

		// Liga-Bonus (1. Liga = 30%, 2. Liga = 20%, 3. Liga = 10%)
		// Wird später implementiert wenn wir teamDivision haben
		
		return newScore > currentScore;
	}
}
