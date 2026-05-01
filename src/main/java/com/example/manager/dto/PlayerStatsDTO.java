package com.example.manager.dto;

import java.util.List;

/**
 * DTO für die detaillierte Spieler-Statistik inkl. letzter Spiele
 */
public class PlayerStatsDTO {
	// Grundlegende Spielerinformationen
	private Long playerId;
	private String name;
	private String position;
	private String country;
	private int age;
	private int rating;
	private int overallPotential;
	private int fitness;
	
	// Vertragsinformationen
	private int contractLength;
	private long salary;
	private long marketValue;
	
	// Gesamt-Statistiken der Saison
	private int matchesPlayed;
	private int totalGoals;
	private int totalAssists;
	private int totalYellowCards;
	private int totalRedCards;
	private Double averageRating; // Durchschnittliche Spielnote
	
	// Letzte Spiele
	private List<PlayerPerformanceDTO> recentPerformances;

	public PlayerStatsDTO() {
	}

	public Long getPlayerId() {
		return playerId;
	}

	public void setPlayerId(Long playerId) {
		this.playerId = playerId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPosition() {
		return position;
	}

	public void setPosition(String position) {
		this.position = position;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public int getRating() {
		return rating;
	}

	public void setRating(int rating) {
		this.rating = rating;
	}

	public int getOverallPotential() {
		return overallPotential;
	}

	public void setOverallPotential(int overallPotential) {
		this.overallPotential = overallPotential;
	}

	public int getFitness() {
		return fitness;
	}

	public void setFitness(int fitness) {
		this.fitness = fitness;
	}

	public int getContractLength() {
		return contractLength;
	}

	public void setContractLength(int contractLength) {
		this.contractLength = contractLength;
	}

	public long getSalary() {
		return salary;
	}

	public void setSalary(long salary) {
		this.salary = salary;
	}

	public long getMarketValue() {
		return marketValue;
	}

	public void setMarketValue(long marketValue) {
		this.marketValue = marketValue;
	}

	public int getMatchesPlayed() {
		return matchesPlayed;
	}

	public void setMatchesPlayed(int matchesPlayed) {
		this.matchesPlayed = matchesPlayed;
	}

	public int getTotalGoals() {
		return totalGoals;
	}

	public void setTotalGoals(int totalGoals) {
		this.totalGoals = totalGoals;
	}

	public int getTotalAssists() {
		return totalAssists;
	}

	public void setTotalAssists(int totalAssists) {
		this.totalAssists = totalAssists;
	}

	public int getTotalYellowCards() {
		return totalYellowCards;
	}

	public void setTotalYellowCards(int totalYellowCards) {
		this.totalYellowCards = totalYellowCards;
	}

	public int getTotalRedCards() {
		return totalRedCards;
	}

	public void setTotalRedCards(int totalRedCards) {
		this.totalRedCards = totalRedCards;
	}

	public Double getAverageRating() {
		return averageRating;
	}

	public void setAverageRating(Double averageRating) {
		this.averageRating = averageRating;
	}

	public List<PlayerPerformanceDTO> getRecentPerformances() {
		return recentPerformances;
	}

	public void setRecentPerformances(List<PlayerPerformanceDTO> recentPerformances) {
		this.recentPerformances = recentPerformances;
	}
}
