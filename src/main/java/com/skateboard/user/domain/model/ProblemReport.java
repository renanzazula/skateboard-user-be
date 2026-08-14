package com.skateboard.user.domain.model;

import java.time.Instant;
import java.util.UUID;

public class ProblemReport {

    private final UUID id;
    private final UUID userId;
    private final ProblemReportCategory category;
    private final String message;
    private final String appVersion;
    private final ProblemReportPlatform platform;
    private final Instant createdAt;

    private ProblemReport(UUID id, UUID userId, ProblemReportCategory category, String message,
                           String appVersion, ProblemReportPlatform platform, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.category = category;
        this.message = message;
        this.appVersion = appVersion;
        this.platform = platform;
        this.createdAt = createdAt;
    }

    public static ProblemReport create(UUID userId, ProblemReportCategory category, String message,
                                        String appVersion, ProblemReportPlatform platform) {
        return new ProblemReport(UUID.randomUUID(), userId, category, message, appVersion, platform, Instant.now());
    }

    public static ProblemReport reconstitute(UUID id, UUID userId, ProblemReportCategory category, String message,
                                              String appVersion, ProblemReportPlatform platform, Instant createdAt) {
        return new ProblemReport(id, userId, category, message, appVersion, platform, createdAt);
    }

    public UUID getId()                             { return id; }
    public UUID getUserId()                         { return userId; }
    public ProblemReportCategory getCategory()      { return category; }
    public String getMessage()                      { return message; }
    public String getAppVersion()                   { return appVersion; }
    public ProblemReportPlatform getPlatform()      { return platform; }
    public Instant getCreatedAt()                   { return createdAt; }
}
