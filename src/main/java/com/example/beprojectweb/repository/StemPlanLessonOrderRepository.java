package com.example.beprojectweb.repository;

import com.example.beprojectweb.entity.StemPlanLessonOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface StemPlanLessonOrderRepository extends JpaRepository<StemPlanLessonOrder, UUID> {
    List<StemPlanLessonOrder> findAllByPlanItemIdOrderByPositionAsc(UUID planItemId);
    List<StemPlanLessonOrder> findAllByPlanItemIdInOrderByPlanItemIdAscPositionAsc(Collection<UUID> planItemIds);
    void deleteAllByPlanItemId(UUID planItemId);
    void deleteAllByPlanItemIdIn(Collection<UUID> planItemIds);
}