package by.lupach.oldtonew2.services;

import by.lupach.oldtonew2.entities.ImportJobRun;
import by.lupach.oldtonew2.entities.ImportError;
import by.lupach.oldtonew2.exceptions.ImportException;
import by.lupach.oldtonew2.repositories.ImportErrorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImportReportService {

    private final ImportErrorRepository errorRepo;

    public void logWarning(ImportJobRun job, String noteGuid, Long patientId, String clientGuid, String msg) {
        log.warn("{} [noteGuid={}, patientId={}, clientGuid={}]", msg, noteGuid, patientId, clientGuid);
        saveError(job, noteGuid, patientId, clientGuid, msg, null);
    }

    public void logError(ImportJobRun job, String noteGuid, Long patientId, String clientGuid, String msg, Exception ex) {
        log.error("{} [noteGuid={}, patientId={}, clientGuid={}]", msg, noteGuid, patientId, clientGuid, ex);
        saveError(job, noteGuid, patientId, clientGuid, msg, ex);
    }

    public void logImportError(ImportJobRun job, String noteGuid, Long patientId, String clientGuid, ImportException ex) {
        log.error("Import error: {} [noteGuid={}, patientId={}, clientGuid={}]",
                ex.getMessage(), noteGuid, patientId, clientGuid, ex);
        saveError(job, noteGuid, patientId, clientGuid, ex.getMessage(), ex);
    }

    private void saveError(ImportJobRun job, String noteGuid, Long patientId, String clientGuid, String msg, Exception ex) {
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