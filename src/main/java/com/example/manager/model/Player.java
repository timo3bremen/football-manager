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

	// Status: Spieler auf Transferliste? (default = false)
	@Column(name = "onTransferList", columnDefinition = "BOOLEAN DEFAULT FALSE")
	private boolean onTransferList = false;

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

	public Player() {
	}

	public Player(String name, int rating, int overallPotential, int form) {
		this.name = name;
		this.rating = rating;
		this.overallPotential = overallPotential;
		this.form = form;
	}

	/** create player with explicit id (used when loading from DB) */
	public Player(long id, String name, int rating, int overallPotential, int form) {
		this.id = id;
		this.name = name;
		this.rating = rating;
		this.overallPotential = overallPotential;
		this.form = form;
	}

	public Player(String name, int rating, int overallPotential, int form, String position) {
		this.name = name;
		this.rating = rating;
		this.overallPotential = overallPotential;
		this.form = form;
		this.position = position;
	}

	public Player(String name, int rating, int overallPotential, int form, String position, String country) {
		this.name = name;
		this.rating = rating;
		this.overallPotential = overallPotential;
		this.form = form;
		this.position = position;
		this.country = country;
	}

	public Player(long id, String name, int rating, int overallPotential, int form, String position, String country) {
		this.id = id;
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
	public void calculateRating() {
		int sum = pace + dribbling + ballControl + shooting + tackling + sliding + heading + crossing + passing
				+ awareness + jumping + stamina + strength;
		this.rating = Math.round(sum / 13.0f);
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
	 * Berechnet den Marktwert basierend auf Alter, Rating und Vertragslaufzeit.
	 * Formel: Base = Rating * 100000 AgeMultiplier = 2.0 - (Age / 35) (jünger =
	 * besser, max 2.0 bei Age 0, min 1.0 bei Age 35+) ContractMultiplier = 0.5 +
	 * (ContractLength / 10) (länger = besser, min 0.5, max 1.5) MarketValue = Base
	 * * AgeMultiplier * ContractMultiplier
	 */
	public void calculateMarketValue() {
		if (rating <= 0) {
			this.marketValue = 0;
			return;
		}

		// Angepasst: rating^3.5 * 0.8 für höhere Exponentialität
		// Rating 40: 100K
		// Rating 50: 559K
		// Rating 60: 1.49M
		// Rating 70: 3.98M
		// Rating 80: 12.1M
		// Rating 90: 34.5M
		long baseValue = (long) (Math.pow(rating, 3.5) * 0.8);

		// Age multiplier: Jüngere Spieler sind mehr wert
		// age 18 -> 3.0, age 24 -> 2.0, age 28 -> 1.5, age 32 -> 1.0
		double ageMultiplier;
		if (age <= 18) {
			ageMultiplier = 3.0;
		} else if (age <= 24) {
			// linear from 3.0 at 18 down to 2.0 at 24
			ageMultiplier = 3.0 - ((age - 18) * (1.0 / 6.0));
		} else if (age <= 28) {
			// linear from 2.0 at 24 down to 1.5 at 28
			ageMultiplier = 2.0 - ((age - 24) * (0.5 / 4.0));
		} else if (age <= 32) {
			// linear from 1.5 at 28 down to 1.0 at 32
			ageMultiplier = 1.5 - ((age - 28) * (0.5 / 4.0));
		} else {
			ageMultiplier = 1.0;
		}

		// Contract multiplier: Längere Verträge sind besser
		// contractLength ist in Saisons (1-5)
		// contractLength 1 = 1.0
		// contractLength 2 = 1.7
		// contractLength 3 = 2.5
		// contractLength 4 = 3.2
		// contractLength 5 = 4.0
		double contractMultiplier;
		switch (contractLength) {
		case 1:
			contractMultiplier = 1.0;
			break;
		case 2:
			contractMultiplier = 1.7;
			break;
		case 3:
			contractMultiplier = 2.5;
			break;
		case 4:
			contractMultiplier = 3.2;
			break;
		case 5:
			contractMultiplier = 4.0;
			break;
		default:
			contractMultiplier = 1.0;
			break;
		}

		this.marketValue = Math.round(baseValue * ageMultiplier * contractMultiplier);
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
	public void initializeSkillsForDivision(int division, java.util.Random random) {
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

		// Initialisiere Potentiale mit etwas höheren Werten (maximal 100)
		int potentialBonus = 5 + random.nextInt(11); // 5-15 höher als die aktuelle Fähigkeit
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

		// Berechne Rating und OverallPotential
		calculateRating();
		calculateOverallPotential();
	}

	/**
	 * Trainiert den Spieler nach einem Spiel. Jede Fähigkeit hat 10% Chance um 1 zu
	 * wachsen, aber nur wenn: - Der aktuelle Wert kleiner als das Potential ist -
	 * Zufallszahl <= 0.1 (10%)
	 */
	public void trainAfterMatch(java.util.Random random) {
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
	 * Anwendung eines Starspieler-Bonus: +5 zu allen Skills und +3 zu allen
	 * Potentialen Dies wird verwendet um 2 zufällige Spieler pro Team als
	 * Star-Spieler zu markieren
	 */
	public void applyStarPlayerBonus() {
		// Erhöhe alle Skills um 5 (maximal 100)
		this.pace = Math.min(100, pace + 5);
		this.dribbling = Math.min(100, dribbling + 5);
		this.ballControl = Math.min(100, ballControl + 5);
		this.shooting = Math.min(100, shooting + 5);
		this.tackling = Math.min(100, tackling + 5);
		this.sliding = Math.min(100, sliding + 5);
		this.heading = Math.min(100, heading + 5);
		this.crossing = Math.min(100, crossing + 5);
		this.passing = Math.min(100, passing + 5);
		this.awareness = Math.min(100, awareness + 5);
		this.jumping = Math.min(100, jumping + 5);
		this.stamina = Math.min(100, stamina + 5);
		this.strength = Math.min(100, strength + 5);

		// Erhöhe alle Potentiale um 3 (maximal 100)
		this.pacePotential = Math.min(100, pacePotential + 3);
		this.dribblingPotential = Math.min(100, dribblingPotential + 3);
		this.ballControlPotential = Math.min(100, ballControlPotential + 3);
		this.shootingPotential = Math.min(100, shootingPotential + 3);
		this.tacklingPotential = Math.min(100, tacklingPotential + 3);
		this.slidingPotential = Math.min(100, slidingPotential + 3);
		this.headingPotential = Math.min(100, headingPotential + 3);
		this.crossingPotential = Math.min(100, crossingPotential + 3);
		this.passingPotential = Math.min(100, passingPotential + 3);
		this.awarenessPotential = Math.min(100, awarenessPotential + 3);
		this.jumpingPotential = Math.min(100, jumpingPotential + 3);
		this.staminaPotential = Math.min(100, staminaPotential + 3);
		this.strengthPotential = Math.min(100, strengthPotential + 3);

		// Berechne Rating und OverallPotential neu
		calculateRating();
		calculateOverallPotential();
	}
}
