package by.lupach.oldtonew.services;

import by.lupach.oldtonew.entities.OldClientGuid;
import by.lupach.oldtonew.repositories.OldClientGuidRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OldClientGuidServiceImpl implements OldClientGuidService {
    private final OldClientGuidRepository oldClientGuidRepository;

    @Override
    public List<OldClientGuid> getAllClientGuids() {
        return oldClientGuidRepository.findAll();
    }

    @Override
    public Optional<OldClientGuid> getByGuid(UUID guid) {
        return oldClientGuidRepository.findByGuid(guid);
    }
}
