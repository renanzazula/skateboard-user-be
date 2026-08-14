package com.skateboard.user.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "problem_reports")
public class ProblemReportJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 30)
    private String category;

    @Column(nullable = false, columnDefinition = "text")
    private String message;

    @Column(name = "app_version", length = 50)
    private String appVersion;

    @Column(length = 20)
    private String platform;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public ProblemReportJpaEntity() {}

    public UUID getId()             { return id; }
    public UUID getUserId()         { return userId; }
    public String getCategory()     { return category; }
    public String getMessage()      { return message; }
    public String getAppVersion()   { return appVersion; }
    public String getPlatform()     { return platform; }
    public Instant getCreatedAt()   { return createdAt; }

    public void setId(UUID id)                    { this.id = id; }
    public void setUserId(UUID userId)             { this.userId = userId; }
    public void setCategory(String category)       { this.category = category; }
    public void setMessage(String message)         { this.message = message; }
    public void setAppVersion(String appVersion)   { this.appVersion = appVersion; }
    public void setPlatform(String platform)       { this.platform = platform; }
    public void setCreatedAt(Instant createdAt)    { this.createdAt = createdAt; }
}
