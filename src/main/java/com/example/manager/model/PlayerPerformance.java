package com.example.manager.model;

import jakarta.persistence.*;

/**
 * Speichert die Leistung eines Spielers in einem bestimmten Spiel
 */
@Entity
@Table(name = "player_performances")
public class PlayerPerformance {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "matchId")
	private Long matchId;

	@Column(name = "playerId")
	private Long playerId;

	@Column(name = "teamId")
	private Long teamId;

	// Spielnote (1.0 = beste Note, 6.0 = schlechteste Note, wie in Deutschland)
	private Double rating;

	// Statistiken
	private int goals;
	private int assists;
	private int yellowCards;
	private int redCards;

	// Spielzeit in Minuten
	private int minutesPlayed;

	public PlayerPerformance() {
	}

	public PlayerPerformance(Long matchId, Long playerId, Long teamId) {
		this.matchId = matchId;
		this.playerId = playerId;
		this.teamId = teamId;
		this.goals = 0;
		this.assists = 0;
		this.yellowCards = 0;
		this.redCards = 0;
		this.minutesPlayed = 90; // Default: volles Spiel
		this.rating = 3.0; // Default: durchschnittliche Note
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getMatchId() {
		return matchId;
	}

	public void setMatchId(Long matchId) {
		this.matchId = matchId;
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

	public Double getRating() {
		return rating;
	}

	public void setRating(Double rating) {
		this.rating = rating;
	}

	public int getGoals() {
		return goals;
	}

	public void setGoals(int goals) {
		this.goals = goals;
	}

	public int getAssists() {
		return assists;
	}

	public void setAssists(int assists) {
		this.assists = assists;
	}

	public int getYellowCards() {
		return yellowCards;
	}

	public void setYellowCards(int yellowCards) {
		this.yellowCards = yellowCards;
	}

	public int getRedCards() {
		return redCards;
	}

	public void setRedCards(int redCards) {
		this.redCards = redCards;
	}

	public int getMinutesPlayed() {
		return minutesPlayed;
	}

	public void setMinutesPlayed(int minutesPlayed) {
		this.minutesPlayed = minutesPlayed;
	}

	/**
	 * Berechnet die Spielnote basierend auf der Performance
	 * Basis: 3.0 (durchschnittlich)
	 * - Tor: -0.5 (besser)
	 * - Vorlage: -0.3 (besser)
	 * - Gelbe Karte: +0.2 (schlechter)
	 * - Rote Karte: +1.0 (schlechter)
	 */
	public void calculateRating() {
		double calculatedRating = 3.0; // Start mit durchschnittlicher Note

		// Positive Ereignisse verbessern die Note (kleinere Zahl)
		calculatedRating -= (goals * 0.5);
		calculatedRating -= (assists * 0.3);

		// Negative Ereignisse verschlechtern die Note (größere Zahl)
		calculatedRating += (yellowCards * 0.2);
		calculatedRating += (redCards * 1.0);

		// Begrenze Note zwischen 1.0 (beste) und 6.0 (schlechteste)
		this.rating = Math.max(1.0, Math.min(6.0, calculatedRating));
	}

	@Override
	public String toString() {
		return "PlayerPerformance{" +
				"id=" + id +
				", matchId=" + matchId +
				", playerId=" + playerId +
				", rating=" + rating +
				", goals=" + goals +
				", assists=" + assists +
				'}';
	}
}
