package com.example.manager.controller;

import com.example.manager.model.SeasonHistory;
import com.example.manager.model.CupHistory;
import com.example.manager.repository.SeasonHistoryRepository;
import com.example.manager.repository.CupHistoryRepository;
import com.example.manager.service.RepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller für die Geschichte-Sektion (vergangene Saisons, Ewige Tabelle)
 */
@RestController
@RequestMapping("/api/v2/history")
@CrossOrigin(origins = "*")
public class HistoryController {

    @Autowired
    private SeasonHistoryRepository seasonHistoryRepository;

    @Autowired
    private CupHistoryRepository cupHistoryRepository;

    @Autowired
    private RepositoryService repositoryService;

    /**
     * Gibt alle verfügbaren Saisons zurück
     * GET /api/v2/history/seasons
     */
    @GetMapping("/seasons")
    public ResponseEntity<List<Integer>> getAvailableSeasons() {
        List<SeasonHistory> allHistory = seasonHistoryRepository.findAll();
        Set<Integer> seasons = allHistory.stream()
                .map(SeasonHistory::getSeason)
                .collect(Collectors.toSet());
        
        List<Integer> sortedSeasons = new ArrayList<>(seasons);
        Collections.sort(sortedSeasons);
        
        return ResponseEntity.ok(sortedSeasons);
    }

    /**
     * Gibt alle Länder für eine Saison zurück
     * GET /api/v2/history/season/{season}/countries
     */
    @GetMapping("/season/{season}/countries")
    public ResponseEntity<List<String>> getCountriesForSeason(@PathVariable int season) {
        List<SeasonHistory> seasonData = seasonHistoryRepository.findBySeason(season);
        Set<String> countries = seasonData.stream()
                .map(SeasonHistory::getCountry)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        return ResponseEntity.ok(new ArrayList<>(countries));
    }

    /**
     * Gibt alle Ligen für eine Saison und ein Land zurück
     * GET /api/v2/history/season/{season}/country/{country}/leagues
     */
    @GetMapping("/season/{season}/country/{country}/leagues")
    public ResponseEntity<List<Map<String, Object>>> getLeaguesForSeason(
            @PathVariable int season, 
            @PathVariable String country) {
        
        List<SeasonHistory> seasonData = seasonHistoryRepository.findBySeasonAndCountry(season, country);
        
        // Gruppiere nach Liga
        Map<String, Map<String, Object>> leagueMap = new HashMap<>();
        for (SeasonHistory h : seasonData) {
            String key = h.getLeagueId() + "_" + h.getDivision();
            if (!leagueMap.containsKey(key)) {
                Map<String, Object> leagueInfo = new HashMap<>();
                leagueInfo.put("leagueId", h.getLeagueId());
                leagueInfo.put("leagueName", h.getLeagueName());
                leagueInfo.put("division", h.getDivision());
                leagueInfo.put("country", h.getCountry());
                leagueMap.put(key, leagueInfo);
            }
        }
        
        List<Map<String, Object>> leagues = new ArrayList<>(leagueMap.values());
        leagues.sort((a, b) -> Integer.compare((Integer) a.get("division"), (Integer) b.get("division")));
        
        return ResponseEntity.ok(leagues);
    }

    /**
     * Gibt die Platzierungen einer Liga in einer Saison zurück
     * GET /api/v2/history/season/{season}/league/{leagueId}
     */
    @GetMapping("/season/{season}/league/{leagueId}")
    public ResponseEntity<List<SeasonHistory>> getLeagueStandings(
            @PathVariable int season,
            @PathVariable Long leagueId) {
        
        List<SeasonHistory> standings = seasonHistoryRepository.findBySeason(season).stream()
                .filter(h -> h.getLeagueId().equals(leagueId))
                .sorted(Comparator.comparingInt(SeasonHistory::getPosition))
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(standings);
    }

    /**
     * Gibt alle Pokalspiele einer Saison für ein Land zurück
     * GET /api/v2/history/season/{season}/cup/{country}
     */
    @GetMapping("/season/{season}/cup/{country}")
    public ResponseEntity<Map<String, Object>> getCupHistory(
            @PathVariable int season,
            @PathVariable String country) {
        
        List<CupHistory> matches = cupHistoryRepository.findBySeasonAndCountryOrderByRoundAsc(season, country);
        
        // Gruppiere nach Runden
        Map<Integer, List<CupHistory>> byRound = matches.stream()
                .collect(Collectors.groupingBy(CupHistory::getRound));
        
        Map<String, Object> result = new HashMap<>();
        result.put("season", season);
        result.put("country", country);
        result.put("rounds", byRound);
        result.put("totalMatches", matches.size());
        
        // Finde Sieger (letztes Spiel der höchsten Runde)
        if (!matches.isEmpty()) {
            int maxRound = matches.stream().mapToInt(CupHistory::getRound).max().orElse(0);
            Optional<CupHistory> finalMatch = matches.stream()
                    .filter(m -> m.getRound() == maxRound)
                    .findFirst();
            
            if (finalMatch.isPresent()) {
                result.put("winner", finalMatch.get().getWinnerName());
                result.put("winnerId", finalMatch.get().getWinnerId());
            }
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * Gibt die Ewige Tabelle für eine Division zurück
     * GET /api/v2/history/all-time-table/{division}
     */
    @GetMapping("/all-time-table/{division}")
    public ResponseEntity<List<Map<String, Object>>> getAllTimeTable(@PathVariable int division) {
        List<SeasonHistory> allHistory = seasonHistoryRepository.findAll().stream()
                .filter(h -> h.getDivision() == division)
                .collect(Collectors.toList());
        
        // Aggregiere nach Team
        Map<Long, Map<String, Object>> teamStats = new HashMap<>();
        
        for (SeasonHistory h : allHistory) {
            Long teamId = h.getTeamId();
            
            if (!teamStats.containsKey(teamId)) {
                Map<String, Object> stats = new HashMap<>();
                stats.put("teamId", teamId);
                stats.put("teamName", h.getTeamName());
                stats.put("seasons", 0);
                stats.put("points", 0);
                stats.put("goalsFor", 0);
                stats.put("goalsAgainst", 0);
                stats.put("wins", 0);
                stats.put("draws", 0);
                stats.put("losses", 0);
                stats.put("titles", 0);
                teamStats.put(teamId, stats);
            }
            
            Map<String, Object> stats = teamStats.get(teamId);
            stats.put("seasons", (Integer) stats.get("seasons") + 1);
            stats.put("points", (Integer) stats.get("points") + h.getPoints());
            stats.put("goalsFor", (Integer) stats.get("goalsFor") + h.getGoalsFor());
            stats.put("goalsAgainst", (Integer) stats.get("goalsAgainst") + h.getGoalsAgainst());
            stats.put("wins", (Integer) stats.get("wins") + h.getWins());
            stats.put("draws", (Integer) stats.get("draws") + h.getDraws());
            stats.put("losses", (Integer) stats.get("losses") + h.getLosses());
            
            if (h.getPosition() == 1) {
                stats.put("titles", (Integer) stats.get("titles") + 1);
            }
        }
        
        List<Map<String, Object>> result = new ArrayList<>(teamStats.values());
        
        // Sortiere nach Punkten, dann nach Tordifferenz
        result.sort((a, b) -> {
            int pointsCompare = Integer.compare((Integer) b.get("points"), (Integer) a.get("points"));
            if (pointsCompare != 0) return pointsCompare;
            
            int goalDiffA = (Integer) a.get("goalsFor") - (Integer) a.get("goalsAgainst");
            int goalDiffB = (Integer) b.get("goalsFor") - (Integer) b.get("goalsAgainst");
            return Integer.compare(goalDiffB, goalDiffA);
        });
        
        // Füge Position hinzu
        for (int i = 0; i < result.size(); i++) {
            result.get(i).put("position", i + 1);
        }
        
        return ResponseEntity.ok(result);
    }
}
