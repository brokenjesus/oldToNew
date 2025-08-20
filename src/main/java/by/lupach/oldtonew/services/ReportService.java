package by.lupach.oldtonew.services;

import by.lupach.oldtonew.entities.ImportJobRun;
import by.lupach.oldtonew.models.ImportStatistics;

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
