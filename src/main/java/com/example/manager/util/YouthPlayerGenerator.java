package com.example.manager.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.example.manager.model.YouthPlayer;

public class YouthPlayerGenerator {

	private static final String[] FIRST_NAMES = { "Luka", "Alex", "Noah", "Matteo", "Leonardo", "Enzo", "Theo", "Lucas",
			"Jan", "Paul", "Marco", "David", "Felix", "Emil", "Milan", "Samu", "José", "Carlos", "Diego", "Pedro",
			"Jorge", "Antonio", "Manuel", "Luis", "Pierre", "Claude", "Jean", "Luc", "André", "Michel", "François",
			"Robert" };

	private static final String[] LAST_NAMES = { "Schmidt", "Müller", "Weber", "Meyer", "Wagner", "Becker", "Schulz",
			"Hoffmann", "Rossi", "Ferrari", "Bianchi", "Romano", "Moretti", "Ferraro", "Gallo", "Costa", "García",
			"Rodríguez", "Martínez", "López", "Hernández", "Pérez", "Sánchez", "Torres", "Dubois", "Moreau", "Bernard",
			"Thomas", "Lambert", "Simon", "Laurent", "Lefebvre" };

	// Länder nach Region gruppiert
	private static final Map<String, String[]> COUNTRIES_BY_REGION = new HashMap<>();

	static {
		COUNTRIES_BY_REGION.put("WestEuropa", new String[] { "Deutschland", "Frankreich", "Niederlande", "Belgien",
				"Österreich", "Luxemburg", "Schweiz" });
		COUNTRIES_BY_REGION.put("Südeuropa", new String[] { "Spanien", "Italien", "Portugal", "Griechenland" });
		COUNTRIES_BY_REGION.put("Osteuropa",
				new String[] { "Polen", "Tschechien", "Ungarn", "Rumänien", "Bulgarien", "Kroatien", "Serbien" });
		COUNTRIES_BY_REGION.put("England", new String[] { "England", "Schottland", "Wales", "Irland" });
		COUNTRIES_BY_REGION.put("Skandinavien",
				new String[] { "Schweden", "Norwegen", "Dänemark", "Finnland", "Island" });
		COUNTRIES_BY_REGION.put("SüdAmerika",
				new String[] { "Brasilien", "Argentinien", "Uruguay", "Paraguay", "Kolumbien", "Venezuela", "Chile" });
		COUNTRIES_BY_REGION.put("Nordamerika", new String[] { "USA", "Mexiko", "Kanada" });
		COUNTRIES_BY_REGION.put("SüdostAsien",
				new String[] { "Thailand", "Vietnam", "Indonesien", "Philippinen", "Malaysia", "Singapur" });
		COUNTRIES_BY_REGION.put("Afrika",
				new String[] { "Kamerun", "Ghana", "Senegal", "Mali", "Elfenbeinküste", "Ägypten", "Nigeria" });
	}

	private static final String[] POSITIONS = { "GK", "DEF", "MID", "FWD" };

	/**
	 * Generiert einen zufälligen Jugenspieler mit Alter 15-18 mit allen
	 * individuellen Skill-Werten und deren Potentialen
	 */
	public static YouthPlayer generateYouthPlayer(Long teamId, Long scoutId) {
		Random random = new Random();

		String name = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)] + " "
				+ LAST_NAMES[random.nextInt(LAST_NAMES.length)];
		String country = COUNTRIES[random.nextInt(COUNTRIES.length)];
		int age = 15 + random.nextInt(4); // 15-18
		String position = POSITIONS[random.nextInt(POSITIONS.length)];

		// Generiere Skills und Potentiale
		SkillSet skillSet = generateSkillSet(random);

		return new YouthPlayer(teamId, scoutId, name, country, age, position, skillSet.pace, skillSet.dribbling,
				skillSet.ballControl, skillSet.shooting, skillSet.tackling, skillSet.sliding, skillSet.heading,
				skillSet.crossing, skillSet.passing, skillSet.awareness, skillSet.jumping, skillSet.stamina,
				skillSet.strength, skillSet.pacePotential, skillSet.dribblingPotential, skillSet.ballControlPotential,
				skillSet.shootingPotential, skillSet.tacklingPotential, skillSet.slidingPotential,
				skillSet.headingPotential, skillSet.crossingPotential, skillSet.passingPotential,
				skillSet.awarenessPotential, skillSet.jumpingPotential, skillSet.staminaPotential,
				skillSet.strengthPotential);
	}

	/**
	 * Generiert einen Jugenspieler basierend auf einer Region mit allen
	 * individuellen Skill-Werten und deren Potentialen
	 */
	public static YouthPlayer generateYouthPlayerForRegion(Long teamId, Long scoutId, String region) {
		Random random = new Random();

		String name = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)] + " "
				+ LAST_NAMES[random.nextInt(LAST_NAMES.length)];

		// Wähle Land aus der Region
		String[] countriesInRegion = COUNTRIES_BY_REGION.getOrDefault(region,
				new String[] { "Deutschland", "Frankreich", "Spanien", "Italien", "Brasilien", "Argentinien" });
		String country = countriesInRegion[random.nextInt(countriesInRegion.length)];

		int age = 15 + random.nextInt(4); // 15-18
		String position = POSITIONS[random.nextInt(POSITIONS.length)];

		// Generiere Skills und Potentiale
		SkillSet skillSet = generateSkillSet(random);

		System.out.println("[YouthPlayerGenerator] Generiere " + name + " - Pace: " + skillSet.pace + "/"
				+ skillSet.pacePotential + ", Rating wird berechnet");

		YouthPlayer player = new YouthPlayer(teamId, scoutId, name, country, age, position, skillSet.pace,
				skillSet.dribbling, skillSet.ballControl, skillSet.shooting, skillSet.tackling, skillSet.sliding,
				skillSet.heading, skillSet.crossing, skillSet.passing, skillSet.awareness, skillSet.jumping,
				skillSet.stamina, skillSet.strength, skillSet.pacePotential, skillSet.dribblingPotential,
				skillSet.ballControlPotential, skillSet.shootingPotential, skillSet.tacklingPotential,
				skillSet.slidingPotential, skillSet.headingPotential, skillSet.crossingPotential,
				skillSet.passingPotential, skillSet.awarenessPotential, skillSet.jumpingPotential,
				skillSet.staminaPotential, skillSet.strengthPotential);

		System.out.println("[YouthPlayerGenerator] Spieler erstellt - Rating: " + player.getRating()
				+ ", OverallPotential: " + player.getOverallPotential());

		return player;
	}

	/**
	 * Generiert einen kompletten SkillSet mit allen 13 Fähigkeiten und deren
	 * Potentialen
	 */
	private static SkillSet generateSkillSet(Random random) {
		// Wähle zufällig 4-6 Peak-Skills (unterschiedliche Indizes)
		boolean[] isPeakSkill = new boolean[13];
		int numPeaks = 4 + random.nextInt(3); // 4, 5 oder 6
		int peaksAdded = 0;

		while (peaksAdded < numPeaks) {
			int randomSkillIndex = random.nextInt(13);
			if (!isPeakSkill[randomSkillIndex]) {
				isPeakSkill[randomSkillIndex] = true;
				peaksAdded++;
			}
		}

		SkillSet skillSet = new SkillSet();

		// Generiere Skills und Potentiale für jeden Skill
		skillSet.pace = generateSkill(isPeakSkill[0], random);
		skillSet.pacePotential = generatePotential(skillSet.pace, random);

		skillSet.dribbling = generateSkill(isPeakSkill[1], random);
		skillSet.dribblingPotential = generatePotential(skillSet.dribbling, random);

		skillSet.ballControl = generateSkill(isPeakSkill[2], random);
		skillSet.ballControlPotential = generatePotential(skillSet.ballControl, random);

		skillSet.shooting = generateSkill(isPeakSkill[3], random);
		skillSet.shootingPotential = generatePotential(skillSet.shooting, random);

		skillSet.tackling = generateSkill(isPeakSkill[4], random);
		skillSet.tacklingPotential = generatePotential(skillSet.tackling, random);

		skillSet.sliding = generateSkill(isPeakSkill[5], random);
		skillSet.slidingPotential = generatePotential(skillSet.sliding, random);

		skillSet.heading = generateSkill(isPeakSkill[6], random);
		skillSet.headingPotential = generatePotential(skillSet.heading, random);

		skillSet.crossing = generateSkill(isPeakSkill[7], random);
		skillSet.crossingPotential = generatePotential(skillSet.crossing, random);

		skillSet.passing = generateSkill(isPeakSkill[8], random);
		skillSet.passingPotential = generatePotential(skillSet.passing, random);

		skillSet.awareness = generateSkill(isPeakSkill[9], random);
		skillSet.awarenessPotential = generatePotential(skillSet.awareness, random);

		skillSet.jumping = generateSkill(isPeakSkill[10], random);
		skillSet.jumpingPotential = generatePotential(skillSet.jumping, random);

		skillSet.stamina = generateSkill(isPeakSkill[11], random);
		skillSet.staminaPotential = generatePotential(skillSet.stamina, random);

		skillSet.strength = generateSkill(isPeakSkill[12], random);
		skillSet.strengthPotential = generatePotential(skillSet.strength, random);

		System.out.println("[YouthPlayerGenerator] SkillSet generiert - " + peaksAdded + " Peak-Skills");

		return skillSet;
	}

	/**
	 * Generiert einen einzelnen Skill (50-65 für Base, 70-85 für Peak)
	 */
	private static int generateSkill(boolean isPeak, Random random) {
		int skill;
		if (isPeak) {
			skill = 70 + random.nextInt(16); // 70-85
		} else {
			skill = 50 + random.nextInt(16); // 50-65
		}
		return Math.max(1, Math.min(100, skill));
	}

	/**
	 * Generiert das Potential für einen Skill (mindestens 5 höher als der Skill)
	 * Mit den Wahrscheinlichkeiten: 60-70: 40%, 70-80: 30%, 80-90: 20%, 90-99: 10%
	 */
	private static int generatePotential(int skill, Random random) {
		int minPotential = skill + 10;
		int potentialBonus;

		if (skill < 50) {
			potentialBonus = random.nextInt(39);
		} else if (skill < 55) {
			potentialBonus = random.nextInt(36);
		} else if (skill < 60) {
			potentialBonus = random.nextInt(33);
		} else if (skill < 65) {
			potentialBonus = random.nextInt(29);
		} else if (skill < 70) {
			potentialBonus = random.nextInt(26);
		} else if (skill < 74) {
			potentialBonus = random.nextInt(21);
		} else if (skill < 78) {
			potentialBonus = random.nextInt(20);
		} else if (skill < 82) {
			potentialBonus = random.nextInt(17);
		} else if (skill < 86) {
			potentialBonus = random.nextInt(14);
		} else if (skill < 89) {
			potentialBonus = random.nextInt(11);
		} else {
			potentialBonus = random.nextInt(5);
		}

		int potential = skill + potentialBonus;

		if (potential < minPotential) {
			potential = minPotential;
		}

		return Math.min(100, potential);
	}

	private static final String[] COUNTRIES = { "Deutschland", "Frankreich", "Spanien", "Italien", "Niederlande",
			"Portugal", "Belgien", "Österreich", "England", "Schottland", "Brasilien", "Argentinien" };

	/**
	 * Hilfsklasse für die Speicherung eines kompletten SkillSets
	 */
	private static class SkillSet {
		int pace, dribbling, ballControl, shooting, tackling, sliding, heading, crossing, passing, awareness, jumping,
				stamina, strength;
		int pacePotential, dribblingPotential, ballControlPotential, shootingPotential, tacklingPotential,
				slidingPotential;
		int headingPotential, crossingPotential, passingPotential, awarenessPotential, jumpingPotential,
				staminaPotential, strengthPotential;
	}
}
