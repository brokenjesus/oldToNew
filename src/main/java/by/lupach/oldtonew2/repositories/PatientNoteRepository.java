package by.lupach.oldtonew2.repositories;


import by.lupach.oldtonew2.entities.OldNoteGuid;
import by.lupach.oldtonew2.entities.PatientNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientNoteRepository extends JpaRepository<PatientNote, Long> {
    Optional<PatientNote> findByOldGuid_Guid(UUID guid);
}