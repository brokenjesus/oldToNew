package by.lupach.oldtonew2.services;

import by.lupach.oldtonew2.entities.OldClientGuid;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface OldClientGuidService {
//    Set<UUID> getMatchingGuids(List<UUID> guids);
    List<OldClientGuid> getAllClientGuids();

    Optional<OldClientGuid> getByGuid(UUID guid);
}
