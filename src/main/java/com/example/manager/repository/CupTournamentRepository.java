package com.example.manager.repository;

import com.example.manager.model.CupTournament;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CupTournamentRepository extends JpaRepository<CupTournament, Long> {
    Optional<CupTournament> findByCountryAndSeason(String country, int season);
    List<CupTournament> findByCountry(String country);
    List<CupTournament> findByStatus(String status);
}
