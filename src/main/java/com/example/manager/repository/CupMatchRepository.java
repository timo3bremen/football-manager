package com.example.manager.repository;

import com.example.manager.model.CupMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CupMatchRepository extends JpaRepository<CupMatch, Long> {
    List<CupMatch> findByTournamentId(Long tournamentId);
    List<CupMatch> findByTournamentIdAndRound(Long tournamentId, int round);
    List<CupMatch> findByTournamentIdAndStatus(Long tournamentId, String status);
    void deleteByTournamentId(Long tournamentId);
}
