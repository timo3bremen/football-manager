package com.example.manager.dto;

import java.util.List;

/**
 * DTO for league statistics with subsections
 */
public class LeagueStatisticsDTO {
	private List<PlayerStatisticsDTO> topScorers;
	private List<CleanSheetDTO> cleanSheets;
	private List<PlayerStatisticsDTO> mostYellowCards;
	private List<PlayerStatisticsDTO> mostRedCards;
	private List<PlayerStatisticsDTO> topAssisters; // Neue Vorlagen-Statistik
	private int totalScorers; // Gesamtzahl Torschützen für Pagination
	private int totalAssisters; // Gesamtzahl Vorlagengeber für Pagination

	public LeagueStatisticsDTO() {
	}

	public LeagueStatisticsDTO(List<PlayerStatisticsDTO> topScorers, List<CleanSheetDTO> cleanSheets,
			List<PlayerStatisticsDTO> mostYellowCards, List<PlayerStatisticsDTO> mostRedCards) {
		this.topScorers = topScorers;
		this.cleanSheets = cleanSheets;
		this.mostYellowCards = mostYellowCards;
		this.mostRedCards = mostRedCards;
	}

	public List<PlayerStatisticsDTO> getTopScorers() {
		return topScorers;
	}

	public void setTopScorers(List<PlayerStatisticsDTO> topScorers) {
		this.topScorers = topScorers;
	}

	public List<CleanSheetDTO> getCleanSheets() {
		return cleanSheets;
	}

	public void setCleanSheets(List<CleanSheetDTO> cleanSheets) {
		this.cleanSheets = cleanSheets;
	}

	public List<PlayerStatisticsDTO> getMostYellowCards() {
		return mostYellowCards;
	}

	public void setMostYellowCards(List<PlayerStatisticsDTO> mostYellowCards) {
		this.mostYellowCards = mostYellowCards;
	}

	public List<PlayerStatisticsDTO> getMostRedCards() {
		return mostRedCards;
	}

	public void setMostRedCards(List<PlayerStatisticsDTO> mostRedCards) {
		this.mostRedCards = mostRedCards;
	}

	public List<PlayerStatisticsDTO> getTopAssisters() {
		return topAssisters;
	}

	public void setTopAssisters(List<PlayerStatisticsDTO> topAssisters) {
		this.topAssisters = topAssisters;
	}

	public int getTotalScorers() {
		return totalScorers;
	}

	public void setTotalScorers(int totalScorers) {
		this.totalScorers = totalScorers;
	}

	public int getTotalAssisters() {
		return totalAssisters;
	}

	public void setTotalAssisters(int totalAssisters) {
		this.totalAssisters = totalAssisters;
	}
}
