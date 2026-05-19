package horse_reserved.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "login_two_factor_challenges",
        indexes = {
                @Index(name = "idx_ltfc_usuario_id",     columnList = "usuario_id"),
                @Index(name = "idx_ltfc_status",         columnList = "status"),
                @Index(name = "idx_ltfc_expires_at",     columnList = "expires_at"),
                @Index(name = "idx_ltfc_usuario_status", columnList = "usuario_id,status")
        }
)
public class LoginTwoFactorChallenge {

    // -----------------------------------------------------------------
    // Campos inmutables (updatable = false)
    // -----------------------------------------------------------------

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private Long usuarioId;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private LocalDateTime expiresAt;

    @Column(name = "max_attempts", nullable = false, updatable = false)
    private int maxAttempts;

    @Column(name = "max_resends", nullable = false, updatable = false)
    private int maxResends;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_ip", length = 50, updatable = false)
    private String createdIp;

    // -----------------------------------------------------------------
    // Campos mutables
    // -----------------------------------------------------------------

    /** Hash BCrypt del OTP actual; se reemplaza en cada reenvío. */
    @Column(name = "otp_hash", nullable = false)
    private String otpHash;

    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    @Column(name = "resend_count", nullable = false)
    private int resendCount = 0;

    @Column(name = "last_sent_at")
    private LocalDateTime lastSentAt;

    @Column(name = "verified_ip", length = 50)
    private String verifiedIp;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TwoFactorChallengeStatus status = TwoFactorChallengeStatus.PENDING;

    // -----------------------------------------------------------------
    // Getters / setters
    // -----------------------------------------------------------------

    public String getId()                              { return id; }
    public void setId(String id)                      { this.id = id; }

    public Long getUsuarioId()                        { return usuarioId; }
    public void setUsuarioId(Long usuarioId)          { this.usuarioId = usuarioId; }

    public LocalDateTime getExpiresAt()               { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public int getMaxAttempts()                       { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts)       { this.maxAttempts = maxAttempts; }

    public int getMaxResends()                        { return maxResends; }
    public void setMaxResends(int maxResends)         { this.maxResends = maxResends; }

    public LocalDateTime getCreatedAt()               { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getCreatedIp()                      { return createdIp; }
    public void setCreatedIp(String createdIp)        { this.createdIp = createdIp; }

    public String getOtpHash()                        { return otpHash; }
    public void setOtpHash(String otpHash)            { this.otpHash = otpHash; }

    public LocalDateTime getConsumedAt()              { return consumedAt; }
    public void setConsumedAt(LocalDateTime t)        { this.consumedAt = t; }

    public int getAttemptCount()                      { return attemptCount; }
    public void setAttemptCount(int attemptCount)     { this.attemptCount = attemptCount; }

    public int getResendCount()                       { return resendCount; }
    public void setResendCount(int resendCount)       { this.resendCount = resendCount; }

    public LocalDateTime getLastSentAt()              { return lastSentAt; }
    public void setLastSentAt(LocalDateTime t)        { this.lastSentAt = t; }

    public String getVerifiedIp()                     { return verifiedIp; }
    public void setVerifiedIp(String verifiedIp)      { this.verifiedIp = verifiedIp; }

    public TwoFactorChallengeStatus getStatus()       { return status; }
    public void setStatus(TwoFactorChallengeStatus s) { this.status = s; }
}