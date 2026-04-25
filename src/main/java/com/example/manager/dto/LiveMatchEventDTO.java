package com.example.manager.dto;

/**
 * DTO für Live-Match-Events während der Simulation
 */
public class LiveMatchEventDTO {
	
	private Long matchId;
	private String type; // "goal", "chance", "injury", "yellow_card", "red_card", "substitution", "match_start", "match_end", "halftime"
	private int minute;
	private String teamName;
	private String playerName;
	private String description;
	private Integer homeGoals;
	private Integer awayGoals;
	
	public LiveMatchEventDTO() {
	}
	
	public LiveMatchEventDTO(Long matchId, String type, int minute, String teamName, String playerName, String description, Integer homeGoals, Integer awayGoals) {
		this.matchId = matchId;
		this.type = type;
		this.minute = minute;
		this.teamName = teamName;
		this.playerName = playerName;
		this.description = description;
		this.homeGoals = homeGoals;
		this.awayGoals = awayGoals;
	}
	
	// Getters and Setters
	public Long getMatchId() {
		return matchId;
	}
	
	public void setMatchId(Long matchId) {
		this.matchId = matchId;
	}
	
	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public int getMinute() {
		return minute;
	}
	
	public void setMinute(int minute) {
		this.minute = minute;
	}
	
	public String getTeamName() {
		return teamName;
	}
	
	public void setTeamName(String teamName) {
		this.teamName = teamName;
	}
	
	public String getPlayerName() {
		return playerName;
	}
	
	public void setPlayerName(String playerName) {
		this.playerName = playerName;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
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
}
