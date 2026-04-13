package com.example.manager.dto;

/**
 * Request für User-Registrierung mit Ligawahl
 */
public class RegistrationRequest {
	private String username;
	private String password;
	private String teamName;
	private Long leagueId; // Die gewählte Liga

	public RegistrationRequest() {
	}

	public RegistrationRequest(String username, String password, String teamName, Long leagueId) {
		this.username = username;
		this.password = password;
		this.teamName = teamName;
		this.leagueId = leagueId;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getTeamName() {
		return teamName;
	}

	public void setTeamName(String teamName) {
		this.teamName = teamName;
	}

	public Long getLeagueId() {
		return leagueId;
	}

	public void setLeagueId(Long leagueId) {
		this.leagueId = leagueId;
	}
}
