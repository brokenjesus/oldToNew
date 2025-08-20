package by.lupach.oldtonew2.repositories;

import by.lupach.oldtonew2.entities.OldClientGuid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface OldClientGuidRepository extends JpaRepository<OldClientGuid, Long> {
//    @Query("SELECT g.guid FROM OldClientGuid g WHERE g.guid IN :guids")
//    Set<UUID> getMatchingGuids(@Param("guids") List<UUID> guids);

    Optional<OldClientGuid> findByGuid(UUID guid);
}