package com.myweb.website_core.interfaces.controller;

import com.myweb.website_core.application.service.business.AnnouncementService;
import com.myweb.website_core.domain.business.entity.Announcement;
import org.springframework.web.bind.annotation.*;
import java.util.concurrent.CompletableFuture;
import java.util.List;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {
    private final AnnouncementService announcementService;
    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }
    @PostMapping
    public CompletableFuture<Announcement> addAnnouncement(@RequestBody Announcement announcement) {
        return announcementService.addAnnouncement(announcement);
    }
    @GetMapping
    public CompletableFuture<List<Announcement>> getAllAnnouncements() {
        return announcementService.getAllAnnouncements();
    }
} 