package com.example.manager.model;

import jakarta.persistence.*;

/**
 * Jugenspieler - wird durch Scouting gefunden
 * 15-16 Jahre: Jugenakademie
 * 17-18 Jahre: Kann zum Kader hinzugefügt werden
 * 
 * Hat die gleichen individuellen Skill-Werte wie ein normaler Player
 */
@Entity
@Table(name = "youth_players")
public class YouthPlayer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "team_id")
    private Long teamId; // Welches Team hat diesen Spieler gescoutet
    
    @Column(name = "scout_id")
    private Long scoutId; // Von welchem Scout wurde er gefunden
    
    private String name;
    private String country;
    private int age; // 15-18
    private String position; // GK, DEF, MID, FWD
    
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
    private int rating; // Durchschnitt aller Fähigkeiten
    private int overallPotential; // Durchschnitt aller Potentiale
    
    @Column(name = "is_in_academy")
    private boolean isInAcademy = false; // In der Jugenakademie?
    
    @Column(name = "is_recruited")
    private boolean isRecruited = false; // In den Kader aufgenommen?
    
    public YouthPlayer() {
    }
    
    public YouthPlayer(Long teamId, Long scoutId, String name, String country, int age, String position,
                      int pace, int dribbling, int ballControl, int shooting, int tackling, int sliding,
                      int heading, int crossing, int passing, int awareness, int jumping, int stamina, int strength,
                      int pacePotential, int dribblingPotential, int ballControlPotential, int shootingPotential,
                      int tacklingPotential, int slidingPotential, int headingPotential, int crossingPotential,
                      int passingPotential, int awarenessPotential, int jumpingPotential, int staminaPotential,
                      int strengthPotential) {
        this.teamId = teamId;
        this.scoutId = scoutId;
        this.name = name;
        this.country = country;
        this.age = age;
        this.position = position;
        this.pace = pace;
        this.dribbling = dribbling;
        this.ballControl = ballControl;
        this.shooting = shooting;
        this.tackling = tackling;
        this.sliding = sliding;
        this.heading = heading;
        this.crossing = crossing;
        this.passing = passing;
        this.awareness = awareness;
        this.jumping = jumping;
        this.stamina = stamina;
        this.strength = strength;
        this.pacePotential = pacePotential;
        this.dribblingPotential = dribblingPotential;
        this.ballControlPotential = ballControlPotential;
        this.shootingPotential = shootingPotential;
        this.tacklingPotential = tacklingPotential;
        this.slidingPotential = slidingPotential;
        this.headingPotential = headingPotential;
        this.crossingPotential = crossingPotential;
        this.passingPotential = passingPotential;
        this.awarenessPotential = awarenessPotential;
        this.jumpingPotential = jumpingPotential;
        this.staminaPotential = staminaPotential;
        this.strengthPotential = strengthPotential;
        
        // Berechne Rating und OverallPotential
        calculateRating();
        calculateOverallPotential();
        
        // Automatisch NICHT in Akademie - muss verpflichtet werden!
        this.isInAcademy = false;
    }
    
    // ... Getter und Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }
    
    public Long getScoutId() { return scoutId; }
    public void setScoutId(Long scoutId) { this.scoutId = scoutId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    
    // Skills
    public int getPace() { return pace; }
    public void setPace(int pace) { this.pace = Math.max(0, Math.min(100, pace)); }
    
    public int getDribbling() { return dribbling; }
    public void setDribbling(int dribbling) { this.dribbling = Math.max(0, Math.min(100, dribbling)); }
    
    public int getBallControl() { return ballControl; }
    public void setBallControl(int ballControl) { this.ballControl = Math.max(0, Math.min(100, ballControl)); }
    
    public int getShooting() { return shooting; }
    public void setShooting(int shooting) { this.shooting = Math.max(0, Math.min(100, shooting)); }
    
    public int getTackling() { return tackling; }
    public void setTackling(int tackling) { this.tackling = Math.max(0, Math.min(100, tackling)); }
    
    public int getSliding() { return sliding; }
    public void setSliding(int sliding) { this.sliding = Math.max(0, Math.min(100, sliding)); }
    
    public int getHeading() { return heading; }
    public void setHeading(int heading) { this.heading = Math.max(0, Math.min(100, heading)); }
    
    public int getCrossing() { return crossing; }
    public void setCrossing(int crossing) { this.crossing = Math.max(0, Math.min(100, crossing)); }
    
    public int getPassing() { return passing; }
    public void setPassing(int passing) { this.passing = Math.max(0, Math.min(100, passing)); }
    
    public int getAwareness() { return awareness; }
    public void setAwareness(int awareness) { this.awareness = Math.max(0, Math.min(100, awareness)); }
    
    public int getJumping() { return jumping; }
    public void setJumping(int jumping) { this.jumping = Math.max(0, Math.min(100, jumping)); }
    
    public int getStamina() { return stamina; }
    public void setStamina(int stamina) { this.stamina = Math.max(0, Math.min(100, stamina)); }
    
    public int getStrength() { return strength; }
    public void setStrength(int strength) { this.strength = Math.max(0, Math.min(100, strength)); }
    
    // Potentiale
    public int getPacePotential() { return pacePotential; }
    public void setPacePotential(int pacePotential) { this.pacePotential = Math.max(0, Math.min(100, pacePotential)); }
    
    public int getDribblingPotential() { return dribblingPotential; }
    public void setDribblingPotential(int dribblingPotential) { this.dribblingPotential = Math.max(0, Math.min(100, dribblingPotential)); }
    
    public int getBallControlPotential() { return ballControlPotential; }
    public void setBallControlPotential(int ballControlPotential) { this.ballControlPotential = Math.max(0, Math.min(100, ballControlPotential)); }
    
    public int getShootingPotential() { return shootingPotential; }
    public void setShootingPotential(int shootingPotential) { this.shootingPotential = Math.max(0, Math.min(100, shootingPotential)); }
    
    public int getTacklingPotential() { return tacklingPotential; }
    public void setTacklingPotential(int tacklingPotential) { this.tacklingPotential = Math.max(0, Math.min(100, tacklingPotential)); }
    
    public int getSlidingPotential() { return slidingPotential; }
    public void setSlidingPotential(int slidingPotential) { this.slidingPotential = Math.max(0, Math.min(100, slidingPotential)); }
    
    public int getHeadingPotential() { return headingPotential; }
    public void setHeadingPotential(int headingPotential) { this.headingPotential = Math.max(0, Math.min(100, headingPotential)); }
    
    public int getCrossingPotential() { return crossingPotential; }
    public void setCrossingPotential(int crossingPotential) { this.crossingPotential = Math.max(0, Math.min(100, crossingPotential)); }
    
    public int getPassingPotential() { return passingPotential; }
    public void setPassingPotential(int passingPotential) { this.passingPotential = Math.max(0, Math.min(100, passingPotential)); }
    
    public int getAwarenessPotential() { return awarenessPotential; }
    public void setAwarenessPotential(int awarenessPotential) { this.awarenessPotential = Math.max(0, Math.min(100, awarenessPotential)); }
    
    public int getJumpingPotential() { return jumpingPotential; }
    public void setJumpingPotential(int jumpingPotential) { this.jumpingPotential = Math.max(0, Math.min(100, jumpingPotential)); }
    
    public int getStaminaPotential() { return staminaPotential; }
    public void setStaminaPotential(int staminaPotential) { this.staminaPotential = Math.max(0, Math.min(100, staminaPotential)); }
    
    public int getStrengthPotential() { return strengthPotential; }
    public void setStrengthPotential(int strengthPotential) { this.strengthPotential = Math.max(0, Math.min(100, strengthPotential)); }
    
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    
    public int getOverallPotential() { return overallPotential; }
    public void setOverallPotential(int overallPotential) { this.overallPotential = overallPotential; }
    
    public boolean isInAcademy() { return isInAcademy; }
    public void setInAcademy(boolean inAcademy) { isInAcademy = inAcademy; }
    
    public boolean isRecruited() { return isRecruited; }
    public void setRecruited(boolean recruited) { isRecruited = recruited; }
    
    // Berechnete Methoden
    public void calculateRating() {
        int sum = pace + dribbling + ballControl + shooting + tackling + sliding + heading + crossing + passing
                + awareness + jumping + stamina + strength;
        this.rating = Math.round(sum / 13.0f);
    }
    
    public void calculateOverallPotential() {
        int sum = pacePotential + dribblingPotential + ballControlPotential + shootingPotential + tacklingPotential
                + slidingPotential + headingPotential + crossingPotential + passingPotential + awarenessPotential
                + jumpingPotential + staminaPotential + strengthPotential;
        this.overallPotential = Math.round(sum / 13.0f);
    }

    /**
     * Trainiert einen Spieler in der Akademie
     * Jeder Skill hat 15% Chance um 1 zu steigen (wenn noch Potential vorhanden)
     */
    public void trainInAcademy(java.util.Random random) {
        final double TRAINING_CHANCE = 0.15; // 15% Chance

        if (pace < pacePotential && random.nextDouble() < TRAINING_CHANCE) {
            pace = Math.min(100, pace + 1);
        }
        if (dribbling < dribblingPotential && random.nextDouble() < TRAINING_CHANCE) {
            dribbling = Math.min(100, dribbling + 1);
        }
        if (ballControl < ballControlPotential && random.nextDouble() < TRAINING_CHANCE) {
            ballControl = Math.min(100, ballControl + 1);
        }
        if (shooting < shootingPotential && random.nextDouble() < TRAINING_CHANCE) {
            shooting = Math.min(100, shooting + 1);
        }
        if (tackling < tacklingPotential && random.nextDouble() < TRAINING_CHANCE) {
            tackling = Math.min(100, tackling + 1);
        }
        if (sliding < slidingPotential && random.nextDouble() < TRAINING_CHANCE) {
            sliding = Math.min(100, sliding + 1);
        }
        if (heading < headingPotential && random.nextDouble() < TRAINING_CHANCE) {
            heading = Math.min(100, heading + 1);
        }
        if (crossing < crossingPotential && random.nextDouble() < TRAINING_CHANCE) {
            crossing = Math.min(100, crossing + 1);
        }
        if (passing < passingPotential && random.nextDouble() < TRAINING_CHANCE) {
            passing = Math.min(100, passing + 1);
        }
        if (awareness < awarenessPotential && random.nextDouble() < TRAINING_CHANCE) {
            awareness = Math.min(100, awareness + 1);
        }
        if (jumping < jumpingPotential && random.nextDouble() < TRAINING_CHANCE) {
            jumping = Math.min(100, jumping + 1);
        }
        if (stamina < staminaPotential && random.nextDouble() < TRAINING_CHANCE) {
            stamina = Math.min(100, stamina + 1);
        }
        if (strength < strengthPotential && random.nextDouble() < TRAINING_CHANCE) {
            strength = Math.min(100, strength + 1);
        }

        // Berechne neues Rating
        calculateRating();
    }
}
