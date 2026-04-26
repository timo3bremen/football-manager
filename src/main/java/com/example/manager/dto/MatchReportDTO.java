package com.example.manager.dto;

import java.util.List;

/**
 * DTO for match report with events
 */
public class MatchReportDTO {
	private Long matchId;
	private Long homeTeamId;
	private Long awayTeamId;
	private String homeTeamName;
	private String awayTeamName;
	private int homeGoals;
	private int awayGoals;
	private String result; // "home", "away", "draw"
	private List<MatchEventDTO> events;
	private Long attendance; // Zuschauerzahl

	public MatchReportDTO() {
	}

	public MatchReportDTO(Long matchId, Long homeTeamId, Long awayTeamId, String homeTeamName, String awayTeamName,
			int homeGoals, int awayGoals, String result, List<MatchEventDTO> events) {
		this.matchId = matchId;
		this.homeTeamId = homeTeamId;
		this.awayTeamId = awayTeamId;
		this.homeTeamName = homeTeamName;
		this.awayTeamName = awayTeamName;
		this.homeGoals = homeGoals;
		this.awayGoals = awayGoals;
		this.result = result;
		this.events = events;
	}

	// ...existing getters and setters...
	public Long getMatchId() {
		return matchId;
	}

	public void setMatchId(Long matchId) {
		this.matchId = matchId;
	}

	public Long getHomeTeamId() {
		return homeTeamId;
	}

	public void setHomeTeamId(Long homeTeamId) {
		this.homeTeamId = homeTeamId;
	}

	public Long getAwayTeamId() {
		return awayTeamId;
	}

	public void setAwayTeamId(Long awayTeamId) {
		this.awayTeamId = awayTeamId;
	}

	public String getHomeTeamName() {
		return homeTeamName;
	}

	public void setHomeTeamName(String homeTeamName) {
		this.homeTeamName = homeTeamName;
	}

	public String getAwayTeamName() {
		return awayTeamName;
	}

	public void setAwayTeamName(String awayTeamName) {
		this.awayTeamName = awayTeamName;
	}

	public int getHomeGoals() {
		return homeGoals;
	}

	public void setHomeGoals(int homeGoals) {
		this.homeGoals = homeGoals;
	}

	public int getAwayGoals() {
		return awayGoals;
	}

	public void setAwayGoals(int awayGoals) {
		this.awayGoals = awayGoals;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

	public List<MatchEventDTO> getEvents() {
		return events;
	}

	public void setEvents(List<MatchEventDTO> events) {
		this.events = events;
	}

	public Long getAttendance() {
		return attendance;
	}

	public void setAttendance(Long attendance) {
		this.attendance = attendance;
	}
}
