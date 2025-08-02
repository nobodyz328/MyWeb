package com.myweb.website_core.infrastructure.mapper;

import com.myweb.website_core.domain.entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
 
@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    // 可扩展自定义查询
} 