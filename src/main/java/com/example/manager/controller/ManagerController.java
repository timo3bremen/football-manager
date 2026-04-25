package com.example.manager.controller;

import com.example.manager.dto.BidRequest;
import com.example.manager.dto.CreateAuctionRequest;
import com.example.manager.dto.LeagueInfoDTO;
import com.example.manager.dto.TeamDetailsDTO;
import com.example.manager.model.Player;
import com.example.manager.model.Team;
import com.example.manager.model.TransferAuction;
import com.example.manager.service.ManagerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ManagerController {

    private final ManagerService service;
    
    @org.springframework.beans.factory.annotation.Value("${dev.authless:false}")
    private boolean devAuthless;

    public ManagerController(ManagerService service) {
        this.service = service;
    }

    @GetMapping("/players")
    public List<Player> players() {
        return service.listPlayers();
    }

    @GetMapping("/teams")
    public List<Team> teams() {
        return service.listTeams();
    }

    @GetMapping("/auctions")
    public List<TransferAuction> auctions() {
        return service.listAuctions();
    }

    @PostMapping("/auctions")
    public ResponseEntity<?> createAuction(@RequestBody CreateAuctionRequest req) {
        try {
            TransferAuction a = service.createAuction(req);
            return ResponseEntity.ok(a);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/auctions/{id}/bid")
    public ResponseEntity<?> bid(@PathVariable("id") long id, @RequestBody BidRequest req) {
        try {
            TransferAuction a = service.placeBid(id, req);
            return ResponseEntity.ok(a);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // admin endpoints for testing
    @PostMapping("/admin/tick")
    public ResponseEntity<?> tick() {
        service.scheduledTick();
        return ResponseEntity.ok("tick processed");
    }

    @PostMapping("/admin/train")
    public ResponseEntity<?> train() {
        service.simulateTrainingTick();
        return ResponseEntity.ok("training tick done");
    }

    @PostMapping("/teams/{id}/state")
    public ResponseEntity<?> saveState(@PathVariable("id") long id, @RequestBody String json, @RequestHeader(value="X-Auth-Token", required=false) String token) {
        // allow dev-mode (no auth) when dev.authless=true
        if (token == null) {
            if (!devAuthless) return ResponseEntity.status(401).body("missing token");
            // in devAuthless mode allow saving without token
            service.saveGameState(id, json);
            return ResponseEntity.ok("saved (dev-mode)");
        }
        // require token and ensure token owner matches team id
        Long ownerTeam = service.getTeamIdForToken(token);
        if (ownerTeam == null || ownerTeam != id) return ResponseEntity.status(403).body("forbidden");
        service.saveGameState(id, json);
        return ResponseEntity.ok("saved");
    }

    @GetMapping("/teams/{id}/state")
    public ResponseEntity<?> getState(@PathVariable("id") long id) {
        String json = service.getGameState(id);
        if (json == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(json);
    }

    // simple team lookup
    @GetMapping("/teams/{id}")
    public ResponseEntity<?> getTeam(@PathVariable("id") long id){
        Team t = service.getTeamById(id);
        if (t == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(t);
    }

    // get team details including strength
    @GetMapping("/teams/{id}/details")
    public ResponseEntity<?> getTeamDetails(@PathVariable("id") long id){
        TeamDetailsDTO details = service.getTeamDetails(id);
        if (details == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(details);
    }

    // update team name
    @PutMapping("/teams/{id}/name")
    public ResponseEntity<?> updateTeamName(@PathVariable("id") long id, @RequestBody java.util.Map<String, String> req){
        try {
            String newName = req.get("name");
            if (newName == null || newName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("name is required");
            }
            Team t = service.updateTeamName(id, newName);
            return ResponseEntity.ok(t);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // auth
    public static class AuthRequest { public String username; public String password; public String teamName; public Long leagueId; }

    @PostMapping("/auth/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest req){
        try{
            String token = service.registerUser(req.username, req.password, req.teamName == null ? "Mein Team" : req.teamName);
            // find team id
            Long tid = service.getTeamIdForToken(token);
            return ResponseEntity.ok(java.util.Map.of("token", token, "teamId", tid));
        }catch(IllegalArgumentException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/auth/register-with-league")
    public ResponseEntity<?> registerWithLeague(@RequestBody AuthRequest req){
        try{
            if (req.leagueId == null) {
                return ResponseEntity.badRequest().body("leagueId is required");
            }
            String token = service.registerUserWithLeague(req.username, req.password, 
                    req.teamName == null ? "Mein Team" : req.teamName, req.leagueId);
            Long tid = service.getTeamIdForToken(token);
            return ResponseEntity.ok(java.util.Map.of("token", token, "teamId", tid));
        }catch(IllegalArgumentException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/auth/leagues")
    public ResponseEntity<?> getAvailableLeagues(){
        try{
            List<LeagueInfoDTO> leagues = service.getAvailableLeagues();
            return ResponseEntity.ok(leagues);
        }catch(Exception e){
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @GetMapping("/auth/countries")
    public ResponseEntity<?> getAvailableCountries(){
        try{
            List<String> countries = service.getAvailableCountries();
            return ResponseEntity.ok(countries);
        }catch(Exception e){
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @GetMapping("/auth/leagues/{country}")
    public ResponseEntity<?> getLeaguesByCountry(@PathVariable("country") String country){
        try{
            List<LeagueInfoDTO> leagues = service.getLeaguesByCountry(country);
            return ResponseEntity.ok(leagues);
        }catch(Exception e){
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest req){
        try{
            String token = service.loginUser(req.username, req.password);
            Long tid = service.getTeamIdForToken(token);
            return ResponseEntity.ok(java.util.Map.of("token", token, "teamId", tid));
        }catch(IllegalArgumentException e){
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    @PostMapping("/admin/clear-users")
    public ResponseEntity<?> clearUsers(){
        service.clearUsers();
        return ResponseEntity.ok("users cleared");
    }
}
