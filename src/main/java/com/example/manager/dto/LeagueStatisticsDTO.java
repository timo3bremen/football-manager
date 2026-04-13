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
}
