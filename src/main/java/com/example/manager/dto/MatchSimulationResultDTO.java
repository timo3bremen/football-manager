package com.example.manager.dto;

/**
 * DTO for match simulation result.
 */
public class MatchSimulationResultDTO {
	private Long matchId;
	private Long homeTeamId;
	private Long awayTeamId;
	private String homeTeamName;
	private String awayTeamName;
	private int homeGoals;
	private int awayGoals;
	private String result; // "home", "away", "draw"

	public MatchSimulationResultDTO(Long matchId, Long homeTeamId, Long awayTeamId, String homeTeamName,
			String awayTeamName, int homeGoals, int awayGoals, String result) {
		this.matchId = matchId;
		this.homeTeamId = homeTeamId;
		this.awayTeamId = awayTeamId;
		this.homeTeamName = homeTeamName;
		this.awayTeamName = awayTeamName;
		this.homeGoals = homeGoals;
		this.awayGoals = awayGoals;
		this.result = result;
	}

	// Getters and Setters
	public Long getMatchId() {
		return matchId;
	}

	public void setMatchId(Long matchId) {
		this.matchId = matchId;
	}

	public Long getHomeTeamId() {
		return homeTeamId;
	}

	public void setHomeTeamId(Long homeTeamId) {
		this.homeTeamId = homeTeamId;
	}

	public Long getAwayTeamId() {
		return awayTeamId;
	}

	public void setAwayTeamId(Long awayTeamId) {
		this.awayTeamId = awayTeamId;
	}

	public String getHomeTeamName() {
		return homeTeamName;
	}

	public void setHomeTeamName(String homeTeamName) {
		this.homeTeamName = homeTeamName;
	}

	public String getAwayTeamName() {
		return awayTeamName;
	}

	public void setAwayTeamName(String awayTeamName) {
		this.awayTeamName = awayTeamName;
	}

	public int getHomeGoals() {
		return homeGoals;
	}

	public void setHomeGoals(int homeGoals) {
		this.homeGoals = homeGoals;
	}

	public int getAwayGoals() {
		return awayGoals;
	}

	public void setAwayGoals(int awayGoals) {
		this.awayGoals = awayGoals;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}
}
