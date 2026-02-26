package org.exemplo.bellory.model.repository.users;

import org.exemplo.bellory.model.entity.users.PasswordResetCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetCodeRepository extends JpaRepository<PasswordResetCode, Long> {

    @Query("SELECT p FROM PasswordResetCode p WHERE p.userId = :userId AND p.userRole = :userRole " +
            "AND p.code = :code AND p.codeUsed = false AND p.codeExpiresAt > CURRENT_TIMESTAMP")
    Optional<PasswordResetCode> findValidCode(@Param("userId") Long userId,
                                               @Param("userRole") String userRole,
                                               @Param("code") String code);

    @Query("SELECT p FROM PasswordResetCode p WHERE p.resetToken = :resetToken " +
            "AND p.tokenUsed = false AND p.tokenExpiresAt > CURRENT_TIMESTAMP")
    Optional<PasswordResetCode> findValidResetToken(@Param("resetToken") String resetToken);

    @Modifying
    @Query("UPDATE PasswordResetCode p SET p.codeUsed = true, p.tokenUsed = true " +
            "WHERE p.userId = :userId AND p.userRole = :userRole")
    void invalidateAllForUser(@Param("userId") Long userId, @Param("userRole") String userRole);
}
