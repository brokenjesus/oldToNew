package by.lupach.oldtonew2.services;

import by.lupach.oldtonew2.entities.ImportError;
import by.lupach.oldtonew2.entities.ImportJobRun;
import by.lupach.oldtonew2.models.ImportStatistics;
import by.lupach.oldtonew2.repositories.ImportErrorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportServiceLoggerImpl implements ReportService {

    private final ImportErrorRepository errorRepo;

    public void logStatistic(ImportStatistics statistics) {
        log.info(statistics.toString());
    }

    public void logError(ImportJobRun job, UUID noteGuid, Long patientId, UUID clientGuid, String msg, Exception ex) {
        log.error("{} [noteGuid={}, patientId={}, clientGuid={}]", msg, noteGuid, patientId, clientGuid, ex);
        saveError(job, noteGuid, patientId, clientGuid, msg, ex);
    }

    private void saveError(ImportJobRun job, UUID noteGuid, Long patientId, UUID clientGuid, String msg, Exception ex) {
        ImportError error = ImportError.builder()
                .jobRun(job)
                .errorTime(LocalDateTime.now())
                .noteGuid(noteGuid)
                .patientId(patientId)
                .clientGuid(clientGuid)
                .message(msg)
                .stacktrace(ex != null ? stackTraceToString(ex) : null)
                .build();
        errorRepo.save(error);
    }

    private static String stackTraceToString(Throwable t) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement e : t.getStackTrace()) {
            sb.append(e.toString()).append('\n');
        }
        return sb.toString();
    }
}