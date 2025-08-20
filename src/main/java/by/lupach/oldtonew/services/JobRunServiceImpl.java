package by.lupach.oldtonew.services;

import by.lupach.oldtonew.entities.ImportJobRun;
import by.lupach.oldtonew.repositories.ImportJobRunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JobRunServiceImpl implements JobRunService {
    private final ImportJobRunRepository jobRunRepo;

    public ImportJobRun save(ImportJobRun job) {
        return jobRunRepo.save(job);
    }
}
