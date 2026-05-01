package com.example.manager.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Speichert die Historie aller Transfers über Auktionen
 */
@Entity
@Table(name = "transfer_history")
public class TransferHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "player_name", nullable = false)
    private String playerName;

    @Column(name = "position", nullable = false)
    private String position;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "age", nullable = false)
    private Integer age;

    @Column(name = "from_team_id")
    private Long fromTeamId;

    @Column(name = "from_team_name")
    private String fromTeamName;

    @Column(name = "to_team_id", nullable = false)
    private Long toTeamId;

    @Column(name = "to_team_name", nullable = false)
    private String toTeamName;

    @Column(name = "transfer_price", nullable = false)
    private Long transferPrice;

    @Column(name = "matchday", nullable = false)
    private Integer matchday;

    @Column(name = "season", nullable = false)
    private Integer season;

    @Column(name = "transfer_time", nullable = false)
    private Instant transferTime;

    public TransferHistory() {
    }

    public TransferHistory(Long playerId, String playerName, String position, Integer rating, Integer age,
                          Long fromTeamId, String fromTeamName, Long toTeamId, String toTeamName,
                          Long transferPrice, Integer matchday, Integer season, Instant transferTime) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.position = position;
        this.rating = rating;
        this.age = age;
        this.fromTeamId = fromTeamId;
        this.fromTeamName = fromTeamName;
        this.toTeamId = toTeamId;
        this.toTeamName = toTeamName;
        this.transferPrice = transferPrice;
        this.matchday = matchday;
        this.season = season;
        this.transferTime = transferTime;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Long getFromTeamId() {
        return fromTeamId;
    }

    public void setFromTeamId(Long fromTeamId) {
        this.fromTeamId = fromTeamId;
    }

    public String getFromTeamName() {
        return fromTeamName;
    }

    public void setFromTeamName(String fromTeamName) {
        this.fromTeamName = fromTeamName;
    }

    public Long getToTeamId() {
        return toTeamId;
    }

    public void setToTeamId(Long toTeamId) {
        this.toTeamId = toTeamId;
    }

    public String getToTeamName() {
        return toTeamName;
    }

    public void setToTeamName(String toTeamName) {
        this.toTeamName = toTeamName;
    }

    public Long getTransferPrice() {
        return transferPrice;
    }

    public void setTransferPrice(Long transferPrice) {
        this.transferPrice = transferPrice;
    }

    public Integer getMatchday() {
        return matchday;
    }

    public void setMatchday(Integer matchday) {
        this.matchday = matchday;
    }

    public Integer getSeason() {
        return season;
    }

    public void setSeason(Integer season) {
        this.season = season;
    }

    public Instant getTransferTime() {
        return transferTime;
    }

    public void setTransferTime(Instant transferTime) {
        this.transferTime = transferTime;
    }
}
