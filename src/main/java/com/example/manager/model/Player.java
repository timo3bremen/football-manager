package com.example.manager.model;

import java.util.Random;

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

	// Status: Spieler auf Transferliste? (default = false)
	@Column(name = "onTransferList", columnDefinition = "BOOLEAN DEFAULT FALSE")
	private boolean onTransferList = false;

	// Status: Spieler ist freier Agent? (kein Verein, kein Ablösegeld)
	@Column(name = "isFreeAgent", columnDefinition = "BOOLEAN DEFAULT FALSE")
	private boolean isFreeAgent = false;

	private String name;

	// Fähigkeiten (1-100)
	private int pace; // Tempo
	private int dribbling; // Dribbeln
	private int ballControl; // Ballkontrolle
	private int shooting; // Schießen
	private int tackling; // Zweikampf
	private int sliding; // Grätsche
	private int heading; // Kopfball
	private int crossing; // Flanken
	private int passing; // Pässe
	private int awareness; // Übersicht
	private int jumping; // Sprungkraft
	private int stamina; // Ausdauer
	private int strength; // Stärke

	// Fitness (0-100, max 100)
	private int fitness = 100; // Default auf 100

	// Potentiale für jede Fähigkeit
	private int pacePotential;
	private int dribblingPotential;
	private int ballControlPotential;
	private int shootingPotential;
	private int tacklingPotential;
	private int slidingPotential;
	private int headingPotential;
	private int crossingPotential;
	private int passingPotential;
	private int awarenessPotential;
	private int jumpingPotential;
	private int staminaPotential;
	private int strengthPotential;

	// Berechnete Werte
	// rating berechnet sich aus Durchschnitt aller Fähigkeiten (ohne Fitness)
	private int rating;

	// overallPotential berechnet sich aus Durchschnitt aller Potentiale
	private int overallPotential;

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

	// contract length in seasons (1-5)
	private int contractLength;

	// Verletzungen
	@Column(name = "isInjured", columnDefinition = "BOOLEAN DEFAULT FALSE")
	private boolean isInjured = false;

	@Column(name = "injuryName")
	private String injuryName; // z.B. "Prellung im Oberschenkel", "Kreuzbandriss"

	@Column(name = "injuryMatchdaysRemaining")
	private int injuryMatchdaysRemaining = 0; // Wie viele Spieltage noch verletzt

	@Column(name = "injuryStartMatchday")
	private int injuryStartMatchday = 0; // Bei welchem Spieltag die Verletzung passiert ist

	// Sperrungen (Rote Karte)
	@Column(name = "isSuspended", columnDefinition = "BOOLEAN DEFAULT FALSE")
	private boolean isSuspended = false;

	@Column(name = "suspensionMatchesRemaining")
	private int suspensionMatchesRemaining = 0; // Wie viele Spiele noch gesperrt

	@Column(name = "suspensionReason")
	private String suspensionReason; // z.B. "Rote Karte"

	public Player() {
	}

	public Player(String name, int rating, int overallPotential, int form) {
		this.name = name;
		this.rating = rating;
		this.overallPotential = overallPotential;
		this.form = form;
	}

	public Player(String name, int rating, int overallPotential, int form, String position, String country) {
		this.name = name;
		this.rating = rating;
		this.overallPotential = overallPotential;
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

	public boolean isOnTransferList() {
		return onTransferList;
	}

	public void setOnTransferList(boolean onTransferList) {
		this.onTransferList = onTransferList;
	}

	public boolean isFreeAgent() {
		return isFreeAgent;
	}

	public void setFreeAgent(boolean freeAgent) {
		isFreeAgent = freeAgent;
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

	// Getter und Setter für Fähigkeiten
	public int getPace() {
		return pace;
	}

	public void setPace(int pace) {
		this.pace = Math.max(0, Math.min(100, pace));
	}

	public int getDribbling() {
		return dribbling;
	}

	public void setDribbling(int dribbling) {
		this.dribbling = Math.max(0, Math.min(100, dribbling));
	}

	public int getBallControl() {
		return ballControl;
	}

	public void setBallControl(int ballControl) {
		this.ballControl = Math.max(0, Math.min(100, ballControl));
	}

	public int getShooting() {
		return shooting;
	}

	public void setShooting(int shooting) {
		this.shooting = Math.max(0, Math.min(100, shooting));
	}

	public int getTackling() {
		return tackling;
	}

	public void setTackling(int tackling) {
		this.tackling = Math.max(0, Math.min(100, tackling));
	}

	public int getSliding() {
		return sliding;
	}

	public void setSliding(int sliding) {
		this.sliding = Math.max(0, Math.min(100, sliding));
	}

	public int getHeading() {
		return heading;
	}

	public void setHeading(int heading) {
		this.heading = Math.max(0, Math.min(100, heading));
	}

	public int getCrossing() {
		return crossing;
	}

	public void setCrossing(int crossing) {
		this.crossing = Math.max(0, Math.min(100, crossing));
	}

	public int getPassing() {
		return passing;
	}

	public void setPassing(int passing) {
		this.passing = Math.max(0, Math.min(100, passing));
	}

	public int getAwareness() {
		return awareness;
	}

	public void setAwareness(int awareness) {
		this.awareness = Math.max(0, Math.min(100, awareness));
	}

	public int getJumping() {
		return jumping;
	}

	public void setJumping(int jumping) {
		this.jumping = Math.max(0, Math.min(100, jumping));
	}

	public int getStamina() {
		return stamina;
	}

	public void setStamina(int stamina) {
		this.stamina = Math.max(0, Math.min(100, stamina));
	}

	public int getStrength() {
		return strength;
	}

	public void setStrength(int strength) {
		this.strength = Math.max(0, Math.min(100, strength));
	}

	// Fitness Getter und Setter
	public int getFitness() {
		return fitness;
	}

	public void setFitness(int fitness) {
		this.fitness = Math.max(0, Math.min(100, fitness));
	}

	// Potentiale Getter und Setter
	public int getPacePotential() {
		return pacePotential;
	}

	public void setPacePotential(int pacePotential) {
		this.pacePotential = Math.max(0, Math.min(100, pacePotential));
	}

	public int getDribblingPotential() {
		return dribblingPotential;
	}

	public void setDribblingPotential(int dribblingPotential) {
		this.dribblingPotential = Math.max(0, Math.min(100, dribblingPotential));
	}

	public int getBallControlPotential() {
		return ballControlPotential;
	}

	public void setBallControlPotential(int ballControlPotential) {
		this.ballControlPotential = Math.max(0, Math.min(100, ballControlPotential));
	}

	public int getShootingPotential() {
		return shootingPotential;
	}

	public void setShootingPotential(int shootingPotential) {
		this.shootingPotential = Math.max(0, Math.min(100, shootingPotential));
	}

	public int getTacklingPotential() {
		return tacklingPotential;
	}

	public void setTacklingPotential(int tacklingPotential) {
		this.tacklingPotential = Math.max(0, Math.min(100, tacklingPotential));
	}

	public int getSlidingPotential() {
		return slidingPotential;
	}

	public void setSlidingPotential(int slidingPotential) {
		this.slidingPotential = Math.max(0, Math.min(100, slidingPotential));
	}

	public int getHeadingPotential() {
		return headingPotential;
	}

	public void setHeadingPotential(int headingPotential) {
		this.headingPotential = Math.max(0, Math.min(100, headingPotential));
	}

	public int getCrossingPotential() {
		return crossingPotential;
	}

	public void setCrossingPotential(int crossingPotential) {
		this.crossingPotential = Math.max(0, Math.min(100, crossingPotential));
	}

	public int getPassingPotential() {
		return passingPotential;
	}

	public void setPassingPotential(int passingPotential) {
		this.passingPotential = Math.max(0, Math.min(100, passingPotential));
	}

	public int getAwarenessPotential() {
		return awarenessPotential;
	}

	public void setAwarenessPotential(int awarenessPotential) {
		this.awarenessPotential = Math.max(0, Math.min(100, awarenessPotential));
	}

	public int getJumpingPotential() {
		return jumpingPotential;
	}

	public void setJumpingPotential(int jumpingPotential) {
		this.jumpingPotential = Math.max(0, Math.min(100, jumpingPotential));
	}

	public int getStaminaPotential() {
		return staminaPotential;
	}

	public void setStaminaPotential(int staminaPotential) {
		this.staminaPotential = Math.max(0, Math.min(100, staminaPotential));
	}

	public int getStrengthPotential() {
		return strengthPotential;
	}

	public void setStrengthPotential(int strengthPotential) {
		this.strengthPotential = Math.max(0, Math.min(100, strengthPotential));
	}

	// Berechnete Ratings
	public int getOverallPotential() {
		return overallPotential;
	}

	public void setOverallPotential(int overallPotential) {
		this.overallPotential = overallPotential;
	}

	/**
	 * Berechnet das Rating aus dem Durchschnitt aller Fähigkeiten (außer Fitness)
	 */
	public int calculateRating() {
		int sum = pace + dribbling + ballControl + shooting + tackling + sliding + heading + crossing + passing
				+ awareness + jumping + stamina + strength;
		this.rating = Math.round(sum / 13.0f);
		return this.rating;
	}

	/**
	 * Berechnet das Gesamt-Potential aus dem Durchschnitt aller Potentiale
	 */
	public void calculateOverallPotential() {
		int sum = pacePotential + dribblingPotential + ballControlPotential + shootingPotential + tacklingPotential
				+ slidingPotential + headingPotential + crossingPotential + passingPotential + awarenessPotential
				+ jumpingPotential + staminaPotential + strengthPotential;
		this.overallPotential = Math.round(sum / 13.0f);
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

	public int getContractLength() {
		return contractLength;
	}

	public void setContractLength(int contractLength) {
		this.contractLength = contractLength;
	}

	/**
	 * Berechnet den Marktwert basierend auf Rating, Alter und Vertragslaufzeit
	 * REALISTISCHE FORMEL mit höheren Werten ab Rating 75+: Rating 50: ~150K Rating
	 * 60: ~500K Rating 70: ~1.5M Rating 75: ~3M Rating 80: ~6M Rating 85: ~12M
	 * Rating 90: ~22M Rating 95: ~40M
	 * 
	 * Multiplikatoren: - Alter (1.0 - 2.5): Jüngere Spieler deutlich wertvoller -
	 * Vertrag (1.0 - 2.0): Längere Verträge wertvoller
	 */
	public void calculateMarketValue() {
		if (rating <= 0) {
			this.marketValue = 0;
			return;
		}

		// Base Value: Detaillierte Tabelle für jedes Rating (40-99)
		// Moderate Progression ohne extreme Sprünge
		long baseValue;
		switch (rating) {
		case 40:
			baseValue = 50_000;
			break;
		case 41:
			baseValue = 60_000;
			break;
		case 42:
			baseValue = 70_000;
			break;
		case 43:
			baseValue = 80_000;
			break;
		case 44:
			baseValue = 90_000;
			break;
		case 45:
			baseValue = 100_000;
			break;
		case 46:
			baseValue = 115_000;
			break;
		case 47:
			baseValue = 130_000;
			break;
		case 48:
			baseValue = 145_000;
			break;
		case 49:
			baseValue = 160_000;
			break;
		case 50:
			baseValue = 180_000;
			break;
		case 51:
			baseValue = 200_000;
			break;
		case 52:
			baseValue = 220_000;
			break;
		case 53:
			baseValue = 245_000;
			break;
		case 54:
			baseValue = 270_000;
			break;
		case 55:
			baseValue = 300_000;
			break;
		case 56:
			baseValue = 330_000;
			break;
		case 57:
			baseValue = 365_000;
			break;
		case 58:
			baseValue = 400_000;
			break;
		case 59:
			baseValue = 440_000;
			break;
		case 60:
			baseValue = 480_000;
			break;
		case 61:
			baseValue = 525_000;
			break;
		case 62:
			baseValue = 575_000;
			break;
		case 63:
			baseValue = 625_000;
			break;
		case 64:
			baseValue = 680_000;
			break;
		case 65:
			baseValue = 740_000;
			break;
		case 66:
			baseValue = 805_000;
			break;
		case 67:
			baseValue = 875_000;
			break;
		case 68:
			baseValue = 950_000;
			break;
		case 69:
			baseValue = 1_030_000;
			break;
		case 70:
			baseValue = 1_120_000;
			break;
		case 71:
			baseValue = 1_220_000;
			break;
		case 72:
			baseValue = 1_330_000;
			break;
		case 73:
			baseValue = 1_450_000;
			break;
		case 74:
			baseValue = 1_580_000;
			break;
		case 75:
			baseValue = 1_720_000;
			break;
		case 76:
			baseValue = 1_870_000;
			break;
		case 77:
			baseValue = 2_030_000;
			break;
		case 78:
			baseValue = 2_200_000;
			break;
		case 79:
			baseValue = 2_390_000;
			break;
		case 80:
			baseValue = 2_590_000;
			break;
		case 81:
			baseValue = 2_810_000;
			break;
		case 82:
			baseValue = 3_050_000;
			break;
		case 83:
			baseValue = 3_310_000;
			break;
		case 84:
			baseValue = 3_590_000;
			break;
		case 85:
			baseValue = 3_900_000;
			break;
		case 86:
			baseValue = 4_240_000;
			break;
		case 87:
			baseValue = 4_610_000;
			break;
		case 88:
			baseValue = 5_020_000;
			break;
		case 89:
			baseValue = 5_470_000;
			break;
		case 90:
			baseValue = 5_970_000;
			break;
		case 91:
			baseValue = 6_520_000;
			break;
		case 92:
			baseValue = 7_130_000;
			break;
		case 93:
			baseValue = 7_800_000;
			break;
		case 94:
			baseValue = 8_540_000;
			break;
		case 95:
			baseValue = 9_360_000;
			break;
		case 96:
			baseValue = 10_270_000;
			break;
		case 97:
			baseValue = 11_280_000;
			break;
		case 98:
			baseValue = 12_400_000;
			break;
		case 99:
			baseValue = 13_650_000;
			break;
		default:
			// Falls außerhalb 40-99: Berechne dynamisch
			if (rating < 40) {
				baseValue = 30_000;
			} else {
				baseValue = 13_650_000; // Max für 99+
			}
			break;
		}

		// Age multiplier: Jüngere Spieler sind wertvoller
		// 16 Jahre = Maximum (3.5x), 36 Jahre = Minimum (0.2x)
		// Linearer Abstieg von 16 bis 36
		double ageMultiplier;
		switch (age) {
		case 16:
			ageMultiplier = 5.0; // Jüngstes Talent - MAXIMUM
			break;
		case 17:
			ageMultiplier = 4.8;
			break;
		case 18:
			ageMultiplier = 4.6;
			break;
		case 19:
			ageMultiplier = 4.4;
			break;
		case 20:
			ageMultiplier = 4.2;
			break;
		case 21:
			ageMultiplier = 4.0;
			break;
		case 22:
			ageMultiplier = 3.8;
			break;
		case 23:
			ageMultiplier = 3.6;
			break;
		case 24:
			ageMultiplier = 3.5;
			break;
		case 25:
			ageMultiplier = 3.4;
			break;
		case 26:
			ageMultiplier = 3.3;
			break;
		case 27:
			ageMultiplier = 3.2;
			break;
		case 28:
			ageMultiplier = 3.1;
			break;
		case 29:
			ageMultiplier = 3.0;
			break;
		case 30:
			ageMultiplier = 2.8;
			break;
		case 31:
			ageMultiplier = 2.6;
			break;
		case 32:
			ageMultiplier = 2.3;
			break;
		case 33:
			ageMultiplier = 2.0;
			break;
		case 34:
			ageMultiplier = 1.6;
			break;
		case 35:
			ageMultiplier = 1.2;
			break;
		case 36:
			ageMultiplier = 0.8; // Fast Karriereende - MINIMUM
			break;
		default:
			// Jünger als 16 oder älter als 36
			if (age < 16) {
				ageMultiplier = 3.5; // Sehr jung (wie 16)
			} else {
				ageMultiplier = 0.1; // Sehr alt (37+)
			}
			break;
		}

		// Contract multiplier: Längere Verträge sind besser
		// contractLength ist in Saisons (1-5)
		// contractLength 1 = 1.0
		// contractLength 2 = 1.25
		// contractLength 3 = 1.5
		// contractLength 4 = 1.75
		// contractLength 5 = 2.0
		double contractMultiplier;
		switch (contractLength) {
		case 1:
			contractMultiplier = 1.0;
			break;
		case 2:
			contractMultiplier = 1.25;
			break;
		case 3:
			contractMultiplier = 1.5;
			break;
		case 4:
			contractMultiplier = 1.75;
			break;
		case 5:
			contractMultiplier = 2.0;
			break;
		default:
			contractMultiplier = 1.0;
			break;
		}

		double potentialMultiplier;
		switch (overallPotential - rating) {
		case 20:
			potentialMultiplier = 3.0;
			break;
		case 19:
			potentialMultiplier = 2.9;
			break;
		case 18:
			potentialMultiplier = 2.8;
			break;
		case 17:
			potentialMultiplier = 2.7;
			break;
		case 16:
			potentialMultiplier = 2.6;
			break;
		case 15:
			potentialMultiplier = 2.5;
			break;
		case 14:
			potentialMultiplier = 2.4;
			break;
		case 13:
			potentialMultiplier = 2.3;
			break;
		case 12:
			potentialMultiplier = 2.2;
			break;
		case 11:
			potentialMultiplier = 2.1;
			break;
		case 10:
			potentialMultiplier = 2.0;
			break;
		case 9:
			potentialMultiplier = 1.9;
			break;
		case 8:
			potentialMultiplier = 1.8;
			break;
		case 7:
			potentialMultiplier = 1.7;
			break;
		case 6:
			potentialMultiplier = 1.6;
			break;
		case 5:
			potentialMultiplier = 1.5;
			break;
		case 4:
			potentialMultiplier = 1.4;
			break;
		case 3:
			potentialMultiplier = 1.3;
			break;
		case 2:
			potentialMultiplier = 1.2;
			break;
		case 1:
			potentialMultiplier = 1.1;
			break;
		case 0:
			potentialMultiplier = 1.0;
			break;
		default:
			potentialMultiplier = 3.0;
			break;
		}

		this.marketValue = Math.round(baseValue * ageMultiplier * contractMultiplier * potentialMultiplier);
	}

	@Override
	public String toString() {
		return "Player{" + "id=" + id + ", name='" + name + '\'' + ", rating=" + rating + ", overallPotential="
				+ overallPotential + ", form=" + form + ", position='" + position + '\'' + ", country='" + country
				+ '\'' + ", age=" + age + ", salary=" + salary + ", marketValue=" + marketValue + '}';
	}

	/**
	 * Initialisiert alle Fähigkeiten und Potentiale mit Zufallswerten basierend auf
	 * Division mit erhöhtem Spread für bessere Verteilung 1. Liga: Durchschnitt
	 * ~75, 2. Liga: Durchschnitt ~65, 3. Liga: Durchschnitt ~55
	 * 
	 * Strategie: Wähle zufällig einige "Hauptfähigkeiten" (4-6) mit höheren Werten
	 * und lasse andere bei niedrigeren Werten, um größere Spieler-Unterschiede zu
	 * erzeugen
	 */
	public void initializeSkillsForDivision(int division, Random random) {
		int baseMin, baseMax, peakMin, peakMax;
		switch (division) {
		case 1:
			baseMin = 65;
			baseMax = 75;
			peakMin = 80;
			peakMax = 95;
			break;
		case 2:
			baseMin = 55;
			baseMax = 65;
			peakMin = 70;
			peakMax = 85;
			break;
		case 3:
			baseMin = 45;
			baseMax = 55;
			peakMin = 60;
			peakMax = 75;
			break;
		default:
			baseMin = 45;
			baseMax = 55;
			peakMin = 60;
			peakMax = 75;
			break;
		}

		// Bestimme zufällig 4-6 "Peak Skills" die höher sein werden
		boolean[] isPeakSkill = new boolean[13];
		int numPeaks = 4 + random.nextInt(3); // 4, 5 oder 6 Peak Skills
		for (int i = 0; i < numPeaks; i++) {
			int randomSkillIndex = random.nextInt(13);
			isPeakSkill[randomSkillIndex] = true;
		}

		// Initialisiere alle 13 Fähigkeiten mit Zufallswerten
		this.pace = isPeakSkill[0] ? (peakMin + random.nextInt(peakMax - peakMin + 1))
				: (baseMin + random.nextInt(baseMax - baseMin + 1));
		this.dribbling = isPeakSkill[1] ? (peakMin + random.nextInt(peakMax - peakMin + 1))
				: (baseMin + random.nextInt(baseMax - baseMin + 1));
		this.ballControl = isPeakSkill[2] ? (peakMin + random.nextInt(peakMax - peakMin + 1))
				: (baseMin + random.nextInt(baseMax - baseMin + 1));
		this.shooting = isPeakSkill[3] ? (peakMin + random.nextInt(peakMax - peakMin + 1))
				: (baseMin + random.nextInt(baseMax - baseMin + 1));
		this.tackling = isPeakSkill[4] ? (peakMin + random.nextInt(peakMax - peakMin + 1))
				: (baseMin + random.nextInt(baseMax - baseMin + 1));
		this.sliding = isPeakSkill[5] ? (peakMin + random.nextInt(peakMax - peakMin + 1))
				: (baseMin + random.nextInt(baseMax - baseMin + 1));
		this.heading = isPeakSkill[6] ? (peakMin + random.nextInt(peakMax - peakMin + 1))
				: (baseMin + random.nextInt(baseMax - baseMin + 1));
		this.crossing = isPeakSkill[7] ? (peakMin + random.nextInt(peakMax - peakMin + 1))
				: (baseMin + random.nextInt(baseMax - baseMin + 1));
		this.passing = isPeakSkill[8] ? (peakMin + random.nextInt(peakMax - peakMin + 1))
				: (baseMin + random.nextInt(baseMax - baseMin + 1));
		this.awareness = isPeakSkill[9] ? (peakMin + random.nextInt(peakMax - peakMin + 1))
				: (baseMin + random.nextInt(baseMax - baseMin + 1));
		this.jumping = isPeakSkill[10] ? (peakMin + random.nextInt(peakMax - peakMin + 1))
				: (baseMin + random.nextInt(baseMax - baseMin + 1));
		this.stamina = isPeakSkill[11] ? (peakMin + random.nextInt(peakMax - peakMin + 1))
				: (baseMin + random.nextInt(baseMax - baseMin + 1));
		this.strength = isPeakSkill[12] ? (peakMin + random.nextInt(peakMax - peakMin + 1))
				: (baseMin + random.nextInt(baseMax - baseMin + 1));

		// Initialisiere Fitness (max 100)
		this.fitness = random.nextInt(101); // 0-100

		// Berechne Rating und OverallPotential
		int rating = calculateRating();

		// Initialisiere Potentiale mit etwas höheren Werten (maximal 100)
		int potentialBonus = 0;

		if (rating < 50) {
			potentialBonus = random.nextInt(35);
		} else if (rating < 55) {
			potentialBonus = random.nextInt(33);
		} else if (rating < 60) {
			potentialBonus = random.nextInt(30);
		} else if (rating < 65) {
			potentialBonus = random.nextInt(26);
		} else if (rating < 70) {
			potentialBonus = random.nextInt(23);
		} else if (rating < 74) {
			potentialBonus = random.nextInt(20);
		} else if (rating < 78) {
			potentialBonus = random.nextInt(17);
		} else if (rating < 82) {
			potentialBonus = random.nextInt(14);
		} else if (rating < 86) {
			potentialBonus = random.nextInt(11);
		} else if (rating < 89) {
			potentialBonus = random.nextInt(8);
		} else {
			potentialBonus = random.nextInt(5);
		}

		// ToDo degrading raitng for old Players (Potential < rating)
		if (this.age > 34) {
			potentialBonus = Math.max(0, potentialBonus - 10);
		} else if (this.age > 33) {
			potentialBonus = Math.max(0, potentialBonus - 9);
		} else if (this.age > 32) {
			potentialBonus = Math.max(0, potentialBonus - 7);
		} else if (this.age > 31) {
			potentialBonus = Math.max(0, potentialBonus - 5);
		} else if (this.age > 30) {
			potentialBonus = Math.max(0, potentialBonus - 3);
		} else if (this.age > 29) {
			potentialBonus = Math.max(0, potentialBonus - 1);
		} else if (this.age > 28) {
			potentialBonus = Math.max(0, potentialBonus);
		} else if (this.age > 27) {
			potentialBonus = Math.max(0, potentialBonus + 1);
		} else if (this.age > 26) {
			potentialBonus = Math.max(0, potentialBonus + 2);
		} else if (this.age > 25) {
			potentialBonus = Math.max(0, potentialBonus + 3);
		} else if (this.age > 24) {
			potentialBonus = Math.max(0, potentialBonus + 4);
		} else if (this.age > 23) {
			potentialBonus = Math.max(0, potentialBonus + 5);
		} else if (this.age > 22) {
			potentialBonus = Math.max(0, potentialBonus + 6);
		} else if (this.age > 21) {
			potentialBonus = Math.max(0, potentialBonus + 7);
		} else if (this.age > 20) {
			potentialBonus = Math.max(0, potentialBonus + 8);
		} else if (this.age > 19) {
			potentialBonus = Math.max(0, potentialBonus + 9);
		} else if (this.age > 18) {
			potentialBonus = Math.max(0, potentialBonus + 10);
		} else {
			potentialBonus = Math.max(0, potentialBonus + 11);
		}

		this.pacePotential = Math.min(100, pace + potentialBonus);
		this.dribblingPotential = Math.min(100, dribbling + potentialBonus);
		this.ballControlPotential = Math.min(100, ballControl + potentialBonus);
		this.shootingPotential = Math.min(100, shooting + potentialBonus);
		this.tacklingPotential = Math.min(100, tackling + potentialBonus);
		this.slidingPotential = Math.min(100, sliding + potentialBonus);
		this.headingPotential = Math.min(100, heading + potentialBonus);
		this.crossingPotential = Math.min(100, crossing + potentialBonus);
		this.passingPotential = Math.min(100, passing + potentialBonus);
		this.awarenessPotential = Math.min(100, awareness + potentialBonus);
		this.jumpingPotential = Math.min(100, jumping + potentialBonus);
		this.staminaPotential = Math.min(100, stamina + potentialBonus);
		this.strengthPotential = Math.min(100, strength + potentialBonus);

		calculateOverallPotential();
	}

	/**
	 * Trainiert den Spieler nach einem Spiel. Jede Fähigkeit hat 10% Chance um 1 zu
	 * wachsen, aber nur wenn: - Der aktuelle Wert kleiner als das Potential ist -
	 * Zufallszahl <= 0.1 (10%)
	 */
	public void trainAfterMatch(Random random) {
		// Tempo
		if (pace < pacePotential && random.nextDouble() <= 0.1) {
			pace = Math.min(100, pace + 1);
		}
		// Dribbeln
		if (dribbling < dribblingPotential && random.nextDouble() <= 0.1) {
			dribbling = Math.min(100, dribbling + 1);
		}
		// Ballkontrolle
		if (ballControl < ballControlPotential && random.nextDouble() <= 0.1) {
			ballControl = Math.min(100, ballControl + 1);
		}
		// Schießen
		if (shooting < shootingPotential && random.nextDouble() <= 0.1) {
			shooting = Math.min(100, shooting + 1);
		}
		// Zweikampf
		if (tackling < tacklingPotential && random.nextDouble() <= 0.1) {
			tackling = Math.min(100, tackling + 1);
		}
		// Grätsche
		if (sliding < slidingPotential && random.nextDouble() <= 0.1) {
			sliding = Math.min(100, sliding + 1);
		}
		// Kopfball
		if (heading < headingPotential && random.nextDouble() <= 0.1) {
			heading = Math.min(100, heading + 1);
		}
		// Flanken
		if (crossing < crossingPotential && random.nextDouble() <= 0.1) {
			crossing = Math.min(100, crossing + 1);
		}
		// Pässe
		if (passing < passingPotential && random.nextDouble() <= 0.1) {
			passing = Math.min(100, passing + 1);
		}
		// Übersicht
		if (awareness < awarenessPotential && random.nextDouble() <= 0.1) {
			awareness = Math.min(100, awareness + 1);
		}
		// Sprungkraft
		if (jumping < jumpingPotential && random.nextDouble() <= 0.1) {
			jumping = Math.min(100, jumping + 1);
		}
		// Ausdauer
		if (stamina < staminaPotential && random.nextDouble() <= 0.1) {
			stamina = Math.min(100, stamina + 1);
		}
		// Stärke
		if (strength < strengthPotential && random.nextDouble() <= 0.1) {
			strength = Math.min(100, strength + 1);
		}

		// Berechne neues Rating nach Training
		calculateRating();
	}

	/**
	 * Verletzung des Spielers eintragen
	 */
	public void injure(String injuryName, int matchdaysOut, int currentMatchday) {
		this.isInjured = true;
		this.injuryName = injuryName;
		this.injuryMatchdaysRemaining = matchdaysOut;
		this.injuryStartMatchday = currentMatchday;
	}

	/**
	 * Reduziert Verletzungsdauer um 1 Spieltag
	 */
	public void decreaseInjury() {
		if (isInjured && injuryMatchdaysRemaining > 0) {
			injuryMatchdaysRemaining--;
			if (injuryMatchdaysRemaining <= 0) {
				isInjured = false;
				injuryName = null;
				injuryMatchdaysRemaining = 0;
			}
		}
	}

	public boolean isInjured() {
		return isInjured;
	}

	public void setInjured(boolean injured) {
		isInjured = injured;
	}

	public String getInjuryName() {
		return injuryName;
	}

	public void setInjuryName(String injuryName) {
		this.injuryName = injuryName;
	}

	public int getInjuryMatchdaysRemaining() {
		return injuryMatchdaysRemaining;
	}

	public void setInjuryMatchdaysRemaining(int injuryMatchdaysRemaining) {
		this.injuryMatchdaysRemaining = injuryMatchdaysRemaining;
	}

	public int getInjuryStartMatchday() {
		return injuryStartMatchday;
	}

	public void setInjuryStartMatchday(int injuryStartMatchday) {
		this.injuryStartMatchday = injuryStartMatchday;
	}

	// === SPERRUNGEN (ROTE KARTE) ===

	/**
	 * Sperrt einen Spieler für eine bestimmte Anzahl von Spielen
	 */
	public void suspend(int matchesOut, String reason) {
		this.isSuspended = true;
		this.suspensionMatchesRemaining = matchesOut;
		this.suspensionReason = reason;
	}

	/**
	 * Reduziert Sperrung um 1 Spiel
	 */
	public void decreaseSuspension() {
		if (isSuspended && suspensionMatchesRemaining > 0) {
			suspensionMatchesRemaining--;
			if (suspensionMatchesRemaining <= 0) {
				isSuspended = false;
				suspensionReason = null;
				suspensionMatchesRemaining = 0;
			}
		}
	}

	public boolean isSuspended() {
		return isSuspended;
	}

	public void setSuspended(boolean suspended) {
		isSuspended = suspended;
	}

	public int getSuspensionMatchesRemaining() {
		return suspensionMatchesRemaining;
	}

	public void setSuspensionMatchesRemaining(int suspensionMatchesRemaining) {
		this.suspensionMatchesRemaining = suspensionMatchesRemaining;
	}

	public String getSuspensionReason() {
		return suspensionReason;
	}

	public void setSuspensionReason(String suspensionReason) {
		this.suspensionReason = suspensionReason;
	}
}
