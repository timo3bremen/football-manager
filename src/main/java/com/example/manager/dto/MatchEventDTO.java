package com.example.manager.dto;

/**
 * DTO for match events (goals, cards)
 */
public class MatchEventDTO {
	private Long id;
	private Long matchId;
	private Long teamId;
	private String teamName;
	private Long playerId;
	private String playerName;
	private String type; // "goal", "yellow_card", "red_card"
	private int minute;

	public MatchEventDTO() {
	}

	public MatchEventDTO(Long id, Long matchId, Long teamId, Long playerId, String playerName, String type,
			int minute) {
		this.id = id;
		this.matchId = matchId;
		this.teamId = teamId;
		this.playerId = playerId;
		this.playerName = playerName;
		this.type = type;
		this.minute = minute;
	}

	public MatchEventDTO(Long id, Long matchId, Long teamId, String teamName, Long playerId, String playerName, String type,
			int minute) {
		this.id = id;
		this.matchId = matchId;
		this.teamId = teamId;
		this.teamName = teamName;
		this.playerId = playerId;
		this.playerName = playerName;
		this.type = type;
		this.minute = minute;
	}

	// ...existing getters and setters...
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

	public Long getTeamId() {
		return teamId;
	}

	public void setTeamId(Long teamId) {
		this.teamId = teamId;
	}

	public String getTeamName() {
		return teamName;
	}

	public void setTeamName(String teamName) {
		this.teamName = teamName;
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
}
