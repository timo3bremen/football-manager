package com.example.manager.repository;

import com.example.manager.model.StadiumPart;
import com.example.manager.model.StadiumPartId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StadiumPartRepository extends JpaRepository<StadiumPart, StadiumPartId> {
    
    List<StadiumPart> findByTeamId(Long teamId);
    
    void deleteByTeamId(Long teamId);
}
