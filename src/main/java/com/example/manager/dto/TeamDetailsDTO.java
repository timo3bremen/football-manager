package com.example.manager.dto;

/**
 * DTO for team details including strength calculation.
 */
public class TeamDetailsDTO {
	private Long teamId;
	private String teamName;
	private int playersInLineup;
	private int totalPlayers;
	private int teamStrength;
	private java.util.List<PlayerLineupDTO> lineup;

	public TeamDetailsDTO(Long teamId, String teamName, int playersInLineup, int totalPlayers, int teamStrength) {
		this.teamId = teamId;
		this.teamName = teamName;
		this.playersInLineup = playersInLineup;
		this.totalPlayers = totalPlayers;
		this.teamStrength = teamStrength;
		this.lineup = new java.util.ArrayList<>();
	}

	// Getters and Setters
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

	public int getPlayersInLineup() {
		return playersInLineup;
	}

	public void setPlayersInLineup(int playersInLineup) {
		this.playersInLineup = playersInLineup;
	}

	public int getTotalPlayers() {
		return totalPlayers;
	}

	public void setTotalPlayers(int totalPlayers) {
		this.totalPlayers = totalPlayers;
	}

	public int getTeamStrength() {
		return teamStrength;
	}

	public void setTeamStrength(int teamStrength) {
		this.teamStrength = teamStrength;
	}

	public java.util.List<PlayerLineupDTO> getLineup() {
		return lineup;
	}

	public void setLineup(java.util.List<PlayerLineupDTO> lineup) {
		this.lineup = lineup;
	}
}
