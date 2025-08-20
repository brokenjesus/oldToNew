package by.lupach.oldtonew.services;

import by.lupach.oldtonew.dtos.OldClientDto;
import by.lupach.oldtonew.dtos.OldNoteDto;
import by.lupach.oldtonew.configs.AppProps;
import by.lupach.oldtonew.entities.ImportJobRun;
import by.lupach.oldtonew.entities.PatientProfile;
import by.lupach.oldtonew.exceptions.ImportException;
import by.lupach.oldtonew.models.ImportStatistics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ImportServiceImpl implements ImportService {
    private final AppProps props;

    private final OldClientApi oldSystemClientService;
    private final PatientService patientService;
    private final NoteService noteService;
    private final ReportService reportService;
    private final JobRunService jobRunService;

    @Autowired
    private ImportStatistics statistics;

    private static final Set<Short> ACTIVE_STATUSES = Set.of((short)210, (short)220, (short)230);

    @Override
    @Transactional
    public void runImport() {
        statistics.reset();
        ImportJobRun job = createJobRun();

        try {
            List<OldClientDto> allOldClients = oldSystemClientService.getAllClients();
            log.info("Import: total clients in old system={}", allOldClients.size());

            List<PatientProfile> savedPatients = patientService.savePatients(allOldClients);
            log.info("Import: saved {} patients", savedPatients.size());

            Map<UUID, PatientProfile> guidToPatient = patientService.buildGuidToPatientMap();

            processActiveClients(job, allOldClients, guidToPatient);

            job.setStatus("SUCCESS");
        } catch (Exception e) {
            job.setStatus("FAILED");
            statistics.incrementError();
            reportService.logError(job, null, null, null, "Fatal error: " + e.getMessage(), e);
            throw new ImportException("Import process failed", e);
        } finally {
            completeJobRun(job);
        }
    }

    private ImportJobRun createJobRun() {
        ImportJobRun job = ImportJobRun.builder()
                .startedAt(LocalDateTime.now())
                .status("RUNNING")
                .build();
        return jobRunService.save(job);
    }

    private void processActiveClients(ImportJobRun job,
                                      List<OldClientDto> clients, Map<UUID, PatientProfile> guidToPatient) {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusYears(props.getLookbackYears());

        for (OldClientDto oldClient : clients) {
            try {
                if (isClientActive(oldClient)) {
                    processClientNotes(job, oldClient, guidToPatient, from, to);
                } else {
                    log.debug("Skipping notes import for inactive client: {}", oldClient.getGuid());
                    statistics.incrementSkippedNotes();
                }
            } catch (Exception e) {
                statistics.incrementError();
                reportService.logError(job, null, null, oldClient.getGuid(),
                        "Failed to process client notes: " + e.getMessage(), e);
            }
        }
    }

    private boolean isClientActive(OldClientDto oldClient) {
        if (oldClient.getStatus() == null) {
            log.warn("Client {} has null status, considering as inactive", oldClient.getGuid());
            return false;
        }

        return ACTIVE_STATUSES.contains(oldClient.getStatus());
    }

    private void processClientNotes(ImportJobRun job, OldClientDto oldClient,
                                    Map<UUID, PatientProfile> guidToPatient, LocalDate from, LocalDate to) {
        PatientProfile patient = guidToPatient.get(oldClient.getGuid());
        if (patient == null) {
            log.warn("Patient not found for GUID: {}", oldClient.getGuid());
            statistics.incrementError();
            reportService.logError(job, null, null, oldClient.getGuid(),
                    "Patient not found in database", null);
            return;
        }

        List<OldNoteDto> notes = fetchClientNotes(oldClient.getAgency(), oldClient.getGuid(), from, to);

        for (OldNoteDto note : notes) {
            try {
                processNote(patient, note);
            } catch (Exception e) {
                statistics.incrementError();
                reportService.logError(job, note.getGuid(), patient.getId(), oldClient.getGuid(),
                        "Failed to process note: " + e.getMessage(), e);
            }
        }
    }

    private List<OldNoteDto> fetchClientNotes(String agency, UUID clientGuid, LocalDate from, LocalDate to) {
        try {
            return oldSystemClientService.getClientNotes(agency, clientGuid, from, to);
        } catch (RuntimeException ex) {
            throw new ImportException("Failed to fetch notes for client: " + clientGuid, ex);
        }
    }

    private void processNote(PatientProfile patient, OldNoteDto note) {
        NoteServiceImpl.ImportResult result = noteService.upsertNote(patient, note);
        updateStatistics(result);
    }

    private void updateStatistics(NoteServiceImpl.ImportResult result) {
        switch (result) {
            case CREATED -> statistics.incrementNewNotes();
            case UPDATED -> statistics.incrementUpdatedNotes();
            case SKIPPED -> statistics.incrementSkippedNotes();
        }
    }

    private void completeJobRun(ImportJobRun job) {
        job.setFinishedAt(LocalDateTime.now());
        job.setNewCount(statistics.getNewNotes());
        job.setUpdatedCount(statistics.getUpdatedNotes());
        job.setSkippedCount(statistics.getSkippedNotes());
        job.setErrorCount(statistics.getErrorCount());
        jobRunService.save(job);

        reportService.logStatistic(statistics);
    }
}