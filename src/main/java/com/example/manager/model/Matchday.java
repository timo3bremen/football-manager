package com.example.manager.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a matchday (e.g. Matchday 1, Matchday 2, etc) with up to 10 matches.
 */
@Entity
@Table(name = "matchdays")
public class Matchday {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "scheduleId")
	private Long scheduleId;

	@Column(name = "leagueId")
	private Long leagueId; // Zum Identifizieren welche Liga dieser Matchday gehört

	private int dayNumber; // 1-38

	@Column(name = "is_off_season")
	private boolean isOffSeason = false; // true for transfer window periods

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "matchdayId")
	private List<Match> matches = new ArrayList<>();

	public Matchday() {
	}

	public Matchday(Long scheduleId, int dayNumber) {
		this.scheduleId = scheduleId;
		this.dayNumber = dayNumber;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getScheduleId() {
		return scheduleId;
	}

	public void setScheduleId(Long scheduleId) {
		this.scheduleId = scheduleId;
	}

	public Long getLeagueId() {
		return leagueId;
	}

	public void setLeagueId(Long leagueId) {
		this.leagueId = leagueId;
	}

	public int getDayNumber() {
		return dayNumber;
	}

	public void setDayNumber(int dayNumber) {
		this.dayNumber = dayNumber;
	}

	public boolean isOffSeason() {
		return isOffSeason;
	}

	public void setIsOffSeason(boolean offSeason) {
		isOffSeason = offSeason;
	}

	public List<Match> getMatches() {
		return matches;
	}

	public void setMatches(List<Match> matches) {
		this.matches = matches;
	}

	@Override
	public String toString() {
		return "Matchday{" + "id=" + id + ", dayNumber=" + dayNumber + ", matches=" + matches.size() + '}';
	}
}
