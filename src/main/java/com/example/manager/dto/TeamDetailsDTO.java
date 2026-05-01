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
	private Integer stadiumCapacity;
	private String country;
	private String leagueName;
	private Long budget; // Für Test-Zwecke
	private java.util.List<PlayerLineupDTO> lineup;
	private java.util.List<PlayerLineupDTO> allPlayers;

	public TeamDetailsDTO(Long teamId, String teamName, int playersInLineup, int totalPlayers, int teamStrength) {
		this.teamId = teamId;
		this.teamName = teamName;
		this.playersInLineup = playersInLineup;
		this.totalPlayers = totalPlayers;
		this.teamStrength = teamStrength;
		this.lineup = new java.util.ArrayList<>();
		this.allPlayers = new java.util.ArrayList<>();
	}

	// ...existing code...
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

	public Integer getStadiumCapacity() {
		return stadiumCapacity;
	}

	public void setStadiumCapacity(Integer stadiumCapacity) {
		this.stadiumCapacity = stadiumCapacity;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getLeagueName() {
		return leagueName;
	}

	public void setLeagueName(String leagueName) {
		this.leagueName = leagueName;
	}

	public java.util.List<PlayerLineupDTO> getAllPlayers() {
		return allPlayers;
	}

	public void setAllPlayers(java.util.List<PlayerLineupDTO> allPlayers) {
		this.allPlayers = allPlayers;
	}

	public Long getBudget() {
		return budget;
	}

	public void setBudget(Long budget) {
		this.budget = budget;
	}
}
