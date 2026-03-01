package com.alert.platform.repository;

import com.alert.platform.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 告警Repository
 *
 * @author Alert Platform Team
 * @version 1.0.0
 */
@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    /**
     * 根据指纹查找告警
     */
    Optional<Alert> findByFingerprint(String fingerprint);

    /**
     * 查找指定时间范围内的告警 (用于去重判断)
     */
    @Query("SELECT a FROM Alert a WHERE a.fingerprint = :fingerprint " +
           "AND a.createdAt > :since")
    Optional<Alert> findByFingerprintAndCreatedAtAfter(
        @Param("fingerprint") String fingerprint,
        @Param("since") LocalDateTime since
    );

    /**
     * 根据批次ID查找告警
     */
    List<Alert> findByBatchIdOrderByCreatedAtAsc(Long batchId);

    /**
     * 根据状态查找告警
     */
    List<Alert> findByStatus(String status);

    /**
     * 根据来源查找告警
     */
    List<Alert> findBySourceOrderByCreatedAtDesc(String source);

    /**
     * 统计批次中的告警数量
     */
    @Query("SELECT COUNT(a) FROM Alert a WHERE a.batchId = :batchId")
    Long countByBatchId(@Param("batchId") Long batchId);

    /**
     * 查找待聚合的告警
     */
    @Query("SELECT a FROM Alert a WHERE a.status = 'PENDING' " +
           "ORDER BY a.createdAt ASC")
    List<Alert> findPendingAlerts();

    /**
     * 批量更新批次中告警的状态
     */
    @Query("UPDATE Alert a SET a.status = :status, a.updatedAt = CURRENT_TIMESTAMP WHERE a.batchId = :batchId")
    @org.springframework.data.jpa.repository.Modifying
    int updateStatusByBatchId(@Param("batchId") Long batchId, @Param("status") String status);

    /**
     * 分页查询指定状态的告警
     */
    org.springframework.data.domain.Page<Alert> findByStatus(String status, org.springframework.data.domain.Pageable pageable);

    /**
     * 统计指定状态的告警数量
     */
    long countByStatus(String status);
}
