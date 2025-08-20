package by.lupach.oldtonew2.services;

import by.lupach.oldtonew2.entities.ImportJobRun;
import by.lupach.oldtonew2.models.ImportStatistics;

import java.util.UUID;

public interface ReportService {
    void logStatistic(ImportStatistics statistics);

    void logError(ImportJobRun job,
                  UUID noteGuid,
                  Long patientId,
                  UUID clientGuid,
                  String msg,
                  Exception ex);
}
