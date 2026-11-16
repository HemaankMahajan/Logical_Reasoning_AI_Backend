package com.AI.writer.app.Repository;

import com.AI.writer.app.Entity.PromptHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PromptHistoryRepository extends JpaRepository<PromptHistory, Long> {

    List<PromptHistory> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT COALESCE(SUM(p.promptTokens), 0), COALESCE(SUM(p.completionTokens), 0), COALESCE(SUM(p.reasoningTokens), 0) " +
            "FROM PromptHistory p WHERE p.userId = :userId AND p.createdAt >= :startDate")
    List<Object[]> getTokenSumSince(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate);

    @Query("SELECT COALESCE(SUM(p.promptTokens), 0), COALESCE(SUM(p.completionTokens), 0), COALESCE(SUM(p.reasoningTokens), 0) " +
            "FROM PromptHistory p WHERE p.userId = :userId AND p.createdAt >= :startDate AND p.createdAt <= :endDate")
    List<Object[]> getTokenSumBetween(@Param("userId") Long userId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COALESCE(SUM(p.promptTokens), 0), COALESCE(SUM(p.completionTokens), 0), COALESCE(SUM(p.reasoningTokens), 0) " +
            "FROM PromptHistory p WHERE p.userId = :userId")
    List<Object[]> getAllTimeTokenSum(@Param("userId") Long userId);

    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM PromptHistory p WHERE p.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}