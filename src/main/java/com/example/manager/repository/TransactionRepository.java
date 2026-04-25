package com.example.manager.repository;

import com.example.manager.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * Find all transactions for a team
     */
    List<Transaction> findByTeamIdOrderByCreatedAtDesc(Long teamId);

    /**
     * Find transactions for a team within a type
     */
    List<Transaction> findByTeamIdAndTypeOrderByCreatedAtDesc(Long teamId, String type);

    /**
     * Delete all transactions for a team
     */
    void deleteByTeamId(Long teamId);
}
