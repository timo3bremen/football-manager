package com.example.manager.repository;

import com.example.manager.model.CupTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CupTeamRepository extends JpaRepository<CupTeam, Long> {
    List<CupTeam> findByTournamentId(Long tournamentId);
    List<CupTeam> findByTournamentIdAndIsActive(Long tournamentId, boolean isActive);
    void deleteByTournamentId(Long tournamentId);
}
