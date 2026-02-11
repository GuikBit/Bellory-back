package org.exemplo.bellory.model.repository.tracking;

import org.exemplo.bellory.model.entity.tracking.TrackingVisitor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface TrackingVisitorRepository extends JpaRepository<TrackingVisitor, UUID> {

    @Modifying
    @Query("UPDATE TrackingVisitor v SET v.lastSeenAt = :lastSeenAt, v.totalSessions = v.totalSessions + 1, v.updatedAt = :lastSeenAt WHERE v.id = :id")
    void incrementSession(@Param("id") UUID id, @Param("lastSeenAt") LocalDateTime lastSeenAt);

    @Modifying
    @Query("UPDATE TrackingVisitor v SET v.totalPageViews = v.totalPageViews + :count, v.updatedAt = NOW() WHERE v.id = :id")
    void incrementPageViews(@Param("id") UUID id, @Param("count") int count);

    @Modifying
    @Query("UPDATE TrackingVisitor v SET v.isConverted = true, v.convertedAt = :convertedAt, v.conversionPlanId = :planId, v.updatedAt = NOW() WHERE v.id = :id")
    void markConverted(@Param("id") UUID id, @Param("convertedAt") LocalDateTime convertedAt, @Param("planId") String planId);

    @Query("SELECT COUNT(DISTINCT v.id) FROM TrackingVisitor v WHERE v.firstSeenAt BETWEEN :start AND :end")
    long countNewVisitors(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(DISTINCT v.id) FROM TrackingVisitor v WHERE v.totalSessions > 1 AND v.lastSeenAt BETWEEN :start AND :end")
    long countReturningVisitors(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(v) FROM TrackingVisitor v WHERE v.lastSeenAt BETWEEN :start AND :end")
    long countVisitorsInPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
