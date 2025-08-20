package by.lupach.oldtonew2.repositories;

import by.lupach.oldtonew2.entities.PatientProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientProfileRepository extends JpaRepository<PatientProfile, Long> {
    @Query("select p from PatientProfile p where p.statusId in (:statuses)")
    List<PatientProfile> findByStatusIdIn(Collection<Short> statuses);

    @Query("SELECT p FROM PatientProfile p JOIN p.oldClientGuids g WHERE g.guid = :guid")
    Optional<PatientProfile> findByGuid(UUID guid);
}
