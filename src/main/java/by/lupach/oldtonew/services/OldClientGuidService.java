package by.lupach.oldtonew.services;

import by.lupach.oldtonew.entities.OldClientGuid;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OldClientGuidService {
    List<OldClientGuid> getAllClientsGuids();

    List<OldClientGuid> getAllClientGuids(Long patientId);

    Optional<OldClientGuid> getByGuid(UUID guid);
}
