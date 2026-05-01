package com.example.manager.repository;

import com.example.manager.model.CupHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CupHistoryRepository extends JpaRepository<CupHistory, Long> {
    List<CupHistory> findBySeason(int season);
    List<CupHistory> findBySeasonAndCountry(int season, String country);
    List<CupHistory> findBySeasonAndCountryOrderByRoundAsc(int season, String country);
}
