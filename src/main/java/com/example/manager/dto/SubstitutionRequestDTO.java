package com.example.manager.dto;

/**
 * DTO für Spieler-Auswechslung während der Live-Simulation
 */
public class SubstitutionRequestDTO {
	
	private Long matchId;
	private Long teamId;
	private Long playerOutId;
	private Long playerInId;
	
	public SubstitutionRequestDTO() {
	}
	
	public SubstitutionRequestDTO(Long matchId, Long teamId, Long playerOutId, Long playerInId) {
		this.matchId = matchId;
		this.teamId = teamId;
		this.playerOutId = playerOutId;
		this.playerInId = playerInId;
	}
	
	// Getters and Setters
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
	
	public Long getPlayerOutId() {
		return playerOutId;
	}
	
	public void setPlayerOutId(Long playerOutId) {
		this.playerOutId = playerOutId;
	}
	
	public Long getPlayerInId() {
		return playerInId;
	}
	
	public void setPlayerInId(Long playerInId) {
		this.playerInId = playerInId;
	}
}
