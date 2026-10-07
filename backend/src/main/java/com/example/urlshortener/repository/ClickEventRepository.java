package com.example.urlshortener.repository;

import com.example.urlshortener.entity.ClickEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {

    long countByShortCode(String shortCode);

    List<ClickEvent> findTop20ByShortCodeOrderByTimestampDesc(String shortCode);

    @Query("SELECT c.browser, COUNT(c) FROM ClickEvent c WHERE c.shortCode = :shortCode GROUP BY c.browser")
    List<Object[]> countClicksByBrowser(@Param("shortCode") String shortCode);

    @Query("SELECT c.deviceType, COUNT(c) FROM ClickEvent c WHERE c.shortCode = :shortCode GROUP BY c.deviceType")
    List<Object[]> countClicksByDeviceType(@Param("shortCode") String shortCode);

    @Query("SELECT c.referrer, COUNT(c) FROM ClickEvent c WHERE c.shortCode = :shortCode GROUP BY c.referrer ORDER BY COUNT(c) DESC")
    List<Object[]> countClicksByReferrer(@Param("shortCode") String shortCode);
}
