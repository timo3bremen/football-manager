package com.example.manager.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * Represents a football league with 12 team slots. Only one league per game (or
 * you can have multiple if needed later).
 */
@Entity
@Table(name = "leagues")
public class League {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String name;
	
	private int division; // 1 = 1. Liga, 2 = 2. Liga, 3 = 3. Liga
	private String divisionLabel; // z.B. "1. Liga", "2. Liga A", "3. Liga B"

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "leagueId")
	private List<LeagueSlot> slots = new ArrayList<>();

	public League() {
	}

	public League(String name) {
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<LeagueSlot> getSlots() {
		return slots;
	}

	public void setSlots(List<LeagueSlot> slots) {
		this.slots = slots;
	}

	public int getDivision() {
		return division;
	}

	public void setDivision(int division) {
		this.division = division;
	}

	public String getDivisionLabel() {
		return divisionLabel;
	}

	public void setDivisionLabel(String divisionLabel) {
		this.divisionLabel = divisionLabel;
	}

	/**
	 * Adds a team to the first available (empty) slot in the league. Returns the
	 * slot if successful, null if no slots available.
	 */
	public LeagueSlot addTeam(Team team) {
		for (LeagueSlot slot : slots) {
			if (slot.getTeamId() == null) {
				slot.setTeamId(team.getId());
				return slot;
			}
		}
		return null; // No empty slots
	}

	/**
	 * Returns the number of filled slots.
	 */
	public int getFilledSlots() {
		return (int) slots.stream().filter(s -> s.getTeamId() != null).count();
	}

	@Override
	public String toString() {
		return "League{" + "id=" + id + ", name='" + name + '\'' + ", slots=" + slots.size() + '}';
	}
}
