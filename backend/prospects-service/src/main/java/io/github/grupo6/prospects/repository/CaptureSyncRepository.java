package io.github.grupo6.prospects.repository;

import io.github.grupo6.prospects.model.CaptureSync;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;

public interface CaptureSyncRepository extends JpaRepository<CaptureSync,UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from CaptureSync s where s.localCaptureId = :id")
    Optional<CaptureSync> locked(UUID id);
}
