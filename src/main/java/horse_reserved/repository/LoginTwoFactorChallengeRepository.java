package horse_reserved.repository;

import horse_reserved.model.LoginTwoFactorChallenge;
import horse_reserved.model.TwoFactorChallengeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LoginTwoFactorChallengeRepository
        extends JpaRepository<LoginTwoFactorChallenge, String> {

    /**
     * Invalida todos los challenges PENDING de un usuario al crear uno nuevo.
     * clearAutomatically limpia el primer nivel de caché para reflejar el cambio
     * en la misma transacción.
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE LoginTwoFactorChallenge c
            SET    c.status = :newStatus
            WHERE  c.usuarioId = :usuarioId
            AND    c.status    = :currentStatus
            """)
    int invalidatePendingByUsuarioId(
            @Param("usuarioId")     Long usuarioId,
            @Param("currentStatus") TwoFactorChallengeStatus currentStatus,
            @Param("newStatus")     TwoFactorChallengeStatus newStatus
    );
}