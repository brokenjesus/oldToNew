package by.lupach.oldtonew2.services;

import by.lupach.oldtonew2.entities.OldClientGuid;
import by.lupach.oldtonew2.repositories.OldClientGuidRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OldClientGuidServiceImpl implements OldClientGuidService {
    private final OldClientGuidRepository oldClientGuidRepository;

//    @Override
//    public Set<UUID> getMatchingGuids(List<UUID> guids) {
//        return oldClientGuidRepository.getMatchingGuids(guids);
//    }

    @Override
    public List<OldClientGuid> getAllClientGuids() {
        return oldClientGuidRepository.findAll();
    }

    @Override
    public Optional<OldClientGuid> getByGuid(UUID guid) {
        return oldClientGuidRepository.findByGuid(guid);
    }
}
