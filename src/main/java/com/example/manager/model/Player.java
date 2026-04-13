package com.example.manager.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Simple player model for the manager game. Attributes are intentionally
 * minimal to be expanded later.
 */
@Entity
@Table(name = "players")
public class Player {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "teamId")
	private Long teamId;

	private String name;

	// rating 10-100
	private int rating;

	// potential growth 0-100
	private int potential;

	// current form modifier -10 .. +10
	private int form;

	// player position: GK, DEF, MID, FWD
	private String position;

	// player country
	private String country;

	// player age in years
	private int age;

	// player salary per season
	private long salary;

	// player market value
	private long marketValue;

	// contract end date (epoch timestamp in milliseconds)
	private long contractEndDate;

	public Player() {
	}

	public Player(String name, int rating, int potential, int form) {
		this.name = name;
		this.rating = rating;
		this.potential = potential;
		this.form = form;
	}

	/** create player with explicit id (used when loading from DB) */
	public Player(long id, String name, int rating, int potential, int form) {
		this.id = id;
		this.name = name;
		this.rating = rating;
		this.potential = potential;
		this.form = form;
	}

	public Player(String name, int rating, int potential, int form, String position) {
		this.name = name;
		this.rating = rating;
		this.potential = potential;
		this.form = form;
		this.position = position;
	}

	public Player(String name, int rating, int potential, int form, String position, String country) {
		this.name = name;
		this.rating = rating;
		this.potential = potential;
		this.form = form;
		this.position = position;
		this.country = country;
	}

	public Player(long id, String name, int rating, int potential, int form, String position, String country) {
		this.id = id;
		this.name = name;
		this.rating = rating;
		this.potential = potential;
		this.form = form;
		this.position = position;
		this.country = country;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getTeamId() {
		return teamId;
	}

	public void setTeamId(Long teamId) {
		this.teamId = teamId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getRating() {
		return rating;
	}

	public void setRating(int rating) {
		this.rating = rating;
	}

	public int getPotential() {
		return potential;
	}

	public void setPotential(int potential) {
		this.potential = potential;
	}

	public int getForm() {
		return form;
	}

	public void setForm(int form) {
		this.form = form;
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

	public long getContractEndDate() {
		return contractEndDate;
	}

	public void setContractEndDate(long contractEndDate) {
		this.contractEndDate = contractEndDate;
	}

	/**
	 * Berechnet den Marktwert basierend auf Alter, Rating und Vertragslaufzeit.
	 * Formel: Base = Rating * 100000 AgeMultiplier = 2.0 - (Age / 35) (jünger =
	 * besser, max 2.0 bei Age 0, min 1.0 bei Age 35+) ContractMultiplier = 0.5 +
	 * (RemainingYears / 10) (länger = besser, min 0.5, max 1.5) MarketValue = Base
	 * * AgeMultiplier * ContractMultiplier
	 */
	public void calculateMarketValue() {
		if (rating <= 0) {
			this.marketValue = 0;
			return;
		}

		// Base value: rating * 100000
		long baseValue = 0;
		if (rating > 80) {
			baseValue = rating * 300000L;
		} else if (rating > 85) {
			baseValue = rating * 500000L;
		} else if (rating > 90) {
			baseValue = rating * 1000000L;
		} else {
			baseValue = rating * 100000L;
		}

		// Age multiplier: piecewise linear mapping with stronger preference for younger players
		// Desired anchors: age 18 -> 4.0, age 24 -> 3.0, age 28 -> 2.0, age 32 -> 1.0
		double ageMultiplier;
		if (age <= 18) {
			ageMultiplier = 4.0;
		} else if (age <= 24) {
			// linear from 4.0 at 18 down to 3.0 at 24
			ageMultiplier = 4.0 - ((age - 18) * (1.0 / 6.0));
		} else if (age <= 28) {
			// linear from 3.0 at 24 down to 2.0 at 28
			ageMultiplier = 3.0 - ((age - 24) * (1.0 / 4.0));
		} else if (age <= 32) {
			// linear from 2.0 at 28 down to 1.0 at 32
			ageMultiplier = 2.0 - ((age - 28) * (1.0 / 4.0));
		} else {
			ageMultiplier = 1.0;
		}

		// Contract multiplier: längere Verträge sind besser
		// Berechne die verbleibenden Jahre ab heute
		long currentTimeMs = System.currentTimeMillis();
		long remainingMs = contractEndDate - currentTimeMs;
		double remainingYears = Math.max(0, remainingMs / (365.25 * 24 * 60 * 60 * 1000));
		double contractMultiplier = Math.min(1.5, 0.5 + (remainingYears / 10.0));

		this.marketValue = Math.round(baseValue * ageMultiplier * contractMultiplier);
	}

	@Override
	public String toString() {
		return "Player{" + "id=" + id + ", name='" + name + '\'' + ", rating=" + rating + ", potential=" + potential
				+ ", form=" + form + ", position='" + position + '\'' + ", country='" + country + '\'' + ", age=" + age
				+ ", salary=" + salary + ", marketValue=" + marketValue + '}';
	}
}
