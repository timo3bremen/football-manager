package com.example.manager.dto;

/**
 * DTO für Spieler-Bewertungen im Spielbericht
 */
public class PlayerRatingDTO {
	private Long playerId;
	private String playerName;
	private String position;
	private double rating; // 1.0 - 6.0
	private int goals;
	private int assists;
	private int chancesCreated;
	private int yellowCards;
	private int redCards;
	private int errors;
	
	public PlayerRatingDTO() {
	}
	
	public PlayerRatingDTO(Long playerId, String playerName, String position, double rating) {
		this.playerId = playerId;
		this.playerName = playerName;
		this.position = position;
		this.rating = rating;
	}

	public Long getPlayerId() {
		return playerId;
	}

	public void setPlayerId(Long playerId) {
		this.playerId = playerId;
	}

	public String getPlayerName() {
		return playerName;
	}

	public void setPlayerName(String playerName) {
		this.playerName = playerName;
	}

	public String getPosition() {
		return position;
	}

	public void setPosition(String position) {
		this.position = position;
	}

	public double getRating() {
		return rating;
	}

	public void setRating(double rating) {
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

	public int getChancesCreated() {
		return chancesCreated;
	}

	public void setChancesCreated(int chancesCreated) {
		this.chancesCreated = chancesCreated;
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

	public int getErrors() {
		return errors;
	}

	public void setErrors(int errors) {
		this.errors = errors;
	}
}
