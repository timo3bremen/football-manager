package com.example.manager.dto;

/**
 * DTO für Ligainformationen (für Dropdown bei Registrierung)
 */
public class LeagueInfoDTO {
    private Long id;
    private String name;
    private String country;
    private int division;
    private String divisionLabel;
    private int filledSlots;
    private int totalSlots;

    public LeagueInfoDTO() {
    }

    public LeagueInfoDTO(Long id, String name, String country, int division, String divisionLabel, int filledSlots, int totalSlots) {
        this.id = id;
        this.name = name;
        this.country = country;
        this.division = division;
        this.divisionLabel = divisionLabel;
        this.filledSlots = filledSlots;
        this.totalSlots = totalSlots;
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

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
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

    public int getFilledSlots() {
        return filledSlots;
    }

    public void setFilledSlots(int filledSlots) {
        this.filledSlots = filledSlots;
    }

    public int getTotalSlots() {
        return totalSlots;
    }

    public void setTotalSlots(int totalSlots) {
        this.totalSlots = totalSlots;
    }
}
