package com.example.manager.dto;

/**
 * Data Transfer Object for league standings/table.
 */
public class LeagueStandingsDTO {
	private Long teamId;
	private String teamName;
	private int position;
	private int played;
	private int won;
	private int drawn;
	private int lost;
	private int goalsFor;
	private int goalsAgainst;
	private int points;
	private int teamStrength;

	public LeagueStandingsDTO(Long teamId, String teamName, int position, int played, int won, int drawn, int lost,
			int goalsFor, int goalsAgainst, int points) {
		this(teamId, teamName, position, played, won, drawn, lost, goalsFor, goalsAgainst, points, 0);
	}

	public LeagueStandingsDTO(Long teamId, String teamName, int position, int played, int won, int drawn, int lost,
			int goalsFor, int goalsAgainst, int points, int teamStrength) {
		this.teamId = teamId;
		this.teamName = teamName;
		this.position = position;
		this.played = played;
		this.won = won;
		this.drawn = drawn;
		this.lost = lost;
		this.goalsFor = goalsFor;
		this.goalsAgainst = goalsAgainst;
		this.points = points;
		this.teamStrength = teamStrength;
	}

	// ... Getters and Setters

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

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	public int getPlayed() {
		return played;
	}

	public void setPlayed(int played) {
		this.played = played;
	}

	public int getWon() {
		return won;
	}

	public void setWon(int won) {
		this.won = won;
	}

	public int getDrawn() {
		return drawn;
	}

	public void setDrawn(int drawn) {
		this.drawn = drawn;
	}

	public int getLost() {
		return lost;
	}

	public void setLost(int lost) {
		this.lost = lost;
	}

	public int getGoalsFor() {
		return goalsFor;
	}

	public void setGoalsFor(int goalsFor) {
		this.goalsFor = goalsFor;
	}

	public int getGoalsAgainst() {
		return goalsAgainst;
	}

	public void setGoalsAgainst(int goalsAgainst) {
		this.goalsAgainst = goalsAgainst;
	}

	public int getPoints() {
		return points;
	}

	public void setPoints(int points) {
		this.points = points;
	}

	public int getTeamStrength() {
		return teamStrength;
	}

	public void setTeamStrength(int teamStrength) {
		this.teamStrength = teamStrength;
	}
}
