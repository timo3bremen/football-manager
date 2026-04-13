package com.example.manager.dto;

/**
 * DTO for player match statistics
 */
public class PlayerStatisticsDTO {
	private Long playerId;
	private String playerName;
	private String position;
	private Long teamId;
	private String teamName;
	private int goals;
	private int yellowCards;
	private int redCards;

	public PlayerStatisticsDTO() {
	}

	public PlayerStatisticsDTO(Long playerId, String playerName, String position, Long teamId, String teamName,
			int goals, int yellowCards, int redCards) {
		this.playerId = playerId;
		this.playerName = playerName;
		this.position = position;
		this.teamId = teamId;
		this.teamName = teamName;
		this.goals = goals;
		this.yellowCards = yellowCards;
		this.redCards = redCards;
	}

	// ...existing getters and setters...
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

	public int getGoals() {
		return goals;
	}

	public void setGoals(int goals) {
		this.goals = goals;
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
}
