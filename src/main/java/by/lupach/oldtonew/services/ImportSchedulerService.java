package by.lupach.oldtonew.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ImportSchedulerService {
    private final ImportService defaultImportService;
    Logger log = LoggerFactory.getLogger("ImportSchedulerService");

    public ImportSchedulerService(ImportServiceImpl importServiceImpl) {
        this.defaultImportService = importServiceImpl;
    }

    @Scheduled(cron = "0 15 1/2 * * *")
    public void scheduled(){
        log.info("Scheduled import started");
        defaultImportService.runImport();
    }
}