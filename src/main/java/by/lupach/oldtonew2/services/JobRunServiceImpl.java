package by.lupach.oldtonew2.services;

import by.lupach.oldtonew2.entities.ImportJobRun;
import by.lupach.oldtonew2.repositories.ImportJobRunRepository;
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
