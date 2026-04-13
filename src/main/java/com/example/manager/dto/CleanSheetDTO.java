package com.example.manager.dto;

/**
 * DTO for clean sheet statistics (team with no goals conceded)
 */
public class CleanSheetDTO {
	private Long teamId;
	private String teamName;
	private int cleanSheets;

	public CleanSheetDTO() {
	}

	public CleanSheetDTO(Long teamId, String teamName, int cleanSheets) {
		this.teamId = teamId;
		this.teamName = teamName;
		this.cleanSheets = cleanSheets;
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

	public int getCleanSheets() {
		return cleanSheets;
	}

	public void setCleanSheets(int cleanSheets) {
		this.cleanSheets = cleanSheets;
	}
}
