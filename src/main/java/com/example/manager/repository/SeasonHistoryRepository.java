package com.example.manager.repository;

import com.example.manager.model.SeasonHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeasonHistoryRepository extends JpaRepository<SeasonHistory, Long> {
    List<SeasonHistory> findBySeason(int season);
    List<SeasonHistory> findBySeasonAndCountry(int season, String country);
    List<SeasonHistory> findBySeasonAndDivision(int season, int division);
    List<SeasonHistory> findBySeasonAndCountryAndDivision(int season, String country, int division);
    List<SeasonHistory> findByTeamIdOrderBySeasonAsc(Long teamId);
}
