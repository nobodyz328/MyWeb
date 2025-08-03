package com.myweb.website_core.application.service.business;

import com.myweb.website_core.domain.business.entity.Announcement;
import com.myweb.website_core.infrastructure.persistence.repository.AnnouncementRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.concurrent.CompletableFuture;
import java.util.List;

@Service
public class AnnouncementService {
    private final AnnouncementRepository announcementRepository;
    public AnnouncementService(AnnouncementRepository announcementRepository) {
        this.announcementRepository = announcementRepository;
    }
    @Async
    public CompletableFuture<Announcement> addAnnouncement(Announcement announcement) {
        // 发布公告逻辑
        return CompletableFuture.completedFuture(null);
    }
    @Async
    public CompletableFuture<List<Announcement>> getAllAnnouncements() {
        // 查询公告逻辑
        return CompletableFuture.completedFuture(null);
    }
} 