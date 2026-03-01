package com.alert.platform.repository;

import com.alert.platform.entity.AlertBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 告警批次Repository
 *
 * @author Alert Platform Team
 * @version 1.0.0
 */
@Repository
public interface AlertBatchRepository extends JpaRepository<AlertBatch, Long> {

    /**
     * 根据批次Key查找
     */
    Optional<AlertBatch> findByBatchKey(String batchKey);

    /**
     * 根据状态查找批次
     */
    List<AlertBatch> findByStatusOrderByCreatedAtDesc(String status);

    /**
     * 查找超时的聚合中批次
     */
    List<AlertBatch> findByStatusAndCreatedAtBefore(
        String status,
        LocalDateTime before
    );

    /**
     * 查找需要处理的批次 (PENDING状态且超过group_wait时间)
     */
    @org.springframework.data.jpa.repository.Query(
        "SELECT b FROM AlertBatch b WHERE b.status = 'PENDING' " +
        "AND b.createdAt < :threshold ORDER BY b.createdAt ASC"
    )
    List<AlertBatch> findBatchesReadyToAggregate(LocalDateTime threshold);

    /**
     * 原子增量更新告警计数 (修复并发计数问题)
     */
    @Modifying
    @Query("UPDATE AlertBatch b SET b.alertCount = b.alertCount + 1 WHERE b.id = :id")
    int incrementAlertCount(@Param("id") Long id);

    /**
     * 统计指定状态的批次数量 (修复全表扫描问题)
     */
    long countByStatus(String status);
}
