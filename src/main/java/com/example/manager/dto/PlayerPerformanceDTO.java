package com.example.manager.dto;

/**
 * DTO für die Anzeige einer Spielerleistung in einem bestimmten Spiel
 */
public class PlayerPerformanceDTO {
	private Long matchId;
	private int matchday;
	private Double rating; // Spielnote (1.0-6.0)
	private int goals;
	private int assists;
	private int yellowCards;
	private int redCards;
	private int minutesPlayed;
	
	// Match-Infos
	private String opponent;
	private boolean isHomeMatch;
	private Integer homeGoals;
	private Integer awayGoals;
	private String result; // "W", "D", "L"

	public PlayerPerformanceDTO() {
	}

	public Long getMatchId() {
		return matchId;
	}

	public void setMatchId(Long matchId) {
		this.matchId = matchId;
	}

	public int getMatchday() {
		return matchday;
	}

	public void setMatchday(int matchday) {
		this.matchday = matchday;
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

	public String getOpponent() {
		return opponent;
	}

	public void setOpponent(String opponent) {
		this.opponent = opponent;
	}

	public boolean isHomeMatch() {
		return isHomeMatch;
	}

	public void setHomeMatch(boolean homeMatch) {
		isHomeMatch = homeMatch;
	}

	public Integer getHomeGoals() {
		return homeGoals;
	}

	public void setHomeGoals(Integer homeGoals) {
		this.homeGoals = homeGoals;
	}

	public Integer getAwayGoals() {
		return awayGoals;
	}

	public void setAwayGoals(Integer awayGoals) {
		this.awayGoals = awayGoals;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}
}
