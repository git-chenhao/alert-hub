package com.alert.platform.repository;

import com.alert.platform.entity.AggregationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 聚合规则Repository
 *
 * @author Alert Platform Team
 * @version 1.0.0
 */
@Repository
public interface AggregationRuleRepository extends JpaRepository<AggregationRule, Long> {

    /**
     * 查找所有启用的规则
     */
    List<AggregationRule> findByEnabledTrueOrderByCreatedAtDesc();

    /**
     * 根据名称查找规则
     */
    AggregationRule findByName(String name);
}
