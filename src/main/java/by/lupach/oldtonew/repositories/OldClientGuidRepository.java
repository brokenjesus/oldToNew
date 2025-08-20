package by.lupach.oldtonew.repositories;

import by.lupach.oldtonew.entities.OldClientGuid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OldClientGuidRepository extends JpaRepository<OldClientGuid, Long> {
    Optional<OldClientGuid> findByGuid(UUID guid);
    List<OldClientGuid> findByPatientProfileId(Long patientId);
}