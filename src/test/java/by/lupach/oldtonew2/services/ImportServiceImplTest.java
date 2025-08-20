package by.lupach.oldtonew2.services;

import by.lupach.oldtonew2.configs.AppProps;
import by.lupach.oldtonew2.dtos.OldClientDto;
import by.lupach.oldtonew2.dtos.OldNoteDto;
import by.lupach.oldtonew2.entities.ImportJobRun;
import by.lupach.oldtonew2.entities.PatientProfile;
import by.lupach.oldtonew2.exceptions.ImportException;
import by.lupach.oldtonew2.models.ImportStatistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImportServiceImplTest {

    @Mock
    private AppProps props;

    @Mock
    private OldClientApi oldSystemClientService;

    @Mock
    private PatientService patientService;

    @Mock
    private NoteService noteService;

    @Mock
    private ReportService reportService;

    @Mock
    private JobRunService jobRunService;

    @InjectMocks
    private ImportServiceImpl importService;

    @Captor
    private ArgumentCaptor<ImportJobRun> jobRunCaptor;

    private OldClientDto activeClient;
    private OldClientDto inactiveClient;
    private OldClientDto clientWithNullStatus;
    private PatientProfile patientProfile;
    private OldNoteDto noteDto;
    private ImportJobRun jobRun;

    @BeforeEach
    void setUp() {
        UUID clientGuid = UUID.randomUUID();
        UUID noteGuid = UUID.randomUUID();

        activeClient = OldClientDto.builder()
                .guid(clientGuid)
                .agency("test-agency")
                .status((short) 210) // активный статус
                .build();

        inactiveClient = OldClientDto.builder()
                .guid(UUID.randomUUID())
                .agency("test-agency")
                .status((short) 100) // неактивный статус
                .build();

        clientWithNullStatus = OldClientDto.builder()
                .guid(UUID.randomUUID())
                .agency("test-agency")
                .status(null)
                .build();

        patientProfile = PatientProfile.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .build();

        noteDto = OldNoteDto.builder()
                .guid(noteGuid)
                .comments("Test note")
                .build();

        jobRun = ImportJobRun.builder()
                .id(1L)
                .startedAt(LocalDateTime.now())
                .status("RUNNING")
                .build();
    }

    @Test
    void runImport_ShouldProcessActiveClientsSuccessfully() {
        // Arrange
        when(props.getLookbackYears()).thenReturn(1);
        when(oldSystemClientService.getAllClients()).thenReturn(List.of(activeClient));
        when(patientService.savePatients(anyList())).thenReturn(List.of(patientProfile));
        when(patientService.buildGuidToPatientMap()).thenReturn(Map.of(activeClient.getGuid(), patientProfile));
        when(oldSystemClientService.getClientNotes(anyString(), any(UUID.class), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(noteDto));
        when(noteService.upsertNote(any(PatientProfile.class), any(OldNoteDto.class)))
                .thenReturn(NoteServiceImpl.ImportResult.CREATED);
        when(jobRunService.save(any(ImportJobRun.class))).thenReturn(jobRun);

        // Act
        importService.runImport();

        // Assert
        verify(jobRunService, times(2)).save(jobRunCaptor.capture());
        verify(oldSystemClientService).getAllClients();
        verify(patientService).savePatients(List.of(activeClient));
        verify(noteService).upsertNote(patientProfile, noteDto);
        verify(reportService).logStatistic(any(ImportStatistics.class));

        ImportJobRun completedJob = jobRunCaptor.getAllValues().get(1);
        assertEquals("SUCCESS", completedJob.getStatus());
        assertNotNull(completedJob.getFinishedAt());
    }

    @Test
    void runImport_ShouldSkipInactiveClients() {
        // Arrange
        when(props.getLookbackYears()).thenReturn(1);
        when(oldSystemClientService.getAllClients()).thenReturn(List.of(inactiveClient));
        when(patientService.savePatients(anyList())).thenReturn(List.of(patientProfile));
        when(patientService.buildGuidToPatientMap()).thenReturn(Map.of(inactiveClient.getGuid(), patientProfile));
        when(jobRunService.save(any(ImportJobRun.class))).thenReturn(jobRun);

        // Act
        importService.runImport();

        // Assert
        verify(oldSystemClientService, never())
                .getClientNotes(anyString(), any(UUID.class), any(LocalDate.class), any(LocalDate.class));
        verify(noteService, never()).upsertNote(any(), any());
    }

    @Test
    void runImport_ShouldSkipClientWithNullStatus() {
        // Arrange
        when(props.getLookbackYears()).thenReturn(1);
        when(oldSystemClientService.getAllClients()).thenReturn(List.of(clientWithNullStatus));
        when(patientService.savePatients(anyList())).thenReturn(List.of(patientProfile));
        when(patientService.buildGuidToPatientMap()).thenReturn(Map.of(clientWithNullStatus.getGuid(), patientProfile));
        when(jobRunService.save(any(ImportJobRun.class))).thenReturn(jobRun);

        // Act
        importService.runImport();

        // Assert
        verify(oldSystemClientService, never())
                .getClientNotes(anyString(), any(UUID.class), any(LocalDate.class), any(LocalDate.class));
        verify(noteService, never()).upsertNote(any(), any());
    }

    @Test
    void runImport_WhenPatientNotFound_ShouldLogError() {
        // Arrange
        when(props.getLookbackYears()).thenReturn(1);
        when(oldSystemClientService.getAllClients()).thenReturn(List.of(activeClient));
        when(patientService.savePatients(anyList())).thenReturn(List.of(patientProfile));
        when(patientService.buildGuidToPatientMap()).thenReturn(Map.of()); // Пустой маппинг - пациент не найден
        when(jobRunService.save(any(ImportJobRun.class))).thenReturn(jobRun);

        // Act
        importService.runImport();

        // Assert
        verify(reportService).logError(
                eq(jobRun),
                isNull(),
                isNull(),
                eq(activeClient.getGuid()),
                eq("Patient not found in database"),
                isNull()
        );
        verify(noteService, never()).upsertNote(any(), any());
    }

    @Test
    void runImport_WhenNoteProcessingFails_ShouldLogError() {
        // Arrange
        when(props.getLookbackYears()).thenReturn(1);
        when(oldSystemClientService.getAllClients()).thenReturn(List.of(activeClient));
        when(patientService.savePatients(anyList())).thenReturn(List.of(patientProfile));
        when(patientService.buildGuidToPatientMap()).thenReturn(Map.of(activeClient.getGuid(), patientProfile));
        when(oldSystemClientService.getClientNotes(anyString(), any(UUID.class), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(noteDto));
        when(noteService.upsertNote(any(PatientProfile.class), any(OldNoteDto.class)))
                .thenThrow(new RuntimeException("Note processing failed"));
        when(jobRunService.save(any(ImportJobRun.class))).thenReturn(jobRun);

        // Act
        importService.runImport();

        // Assert
        verify(reportService).logError(
                eq(jobRun),
                eq(noteDto.getGuid()),
                eq(patientProfile.getId()),
                eq(activeClient.getGuid()),
                contains("Failed to process note"),
                any(RuntimeException.class)
        );
    }

    @Test
    void runImport_WhenFetchNotesFails_ShouldLogError() {
        // Arrange
        when(props.getLookbackYears()).thenReturn(1);
        when(oldSystemClientService.getAllClients()).thenReturn(List.of(activeClient));
        when(patientService.savePatients(anyList())).thenReturn(List.of(patientProfile));
        when(patientService.buildGuidToPatientMap()).thenReturn(Map.of(activeClient.getGuid(), patientProfile));
        when(oldSystemClientService.getClientNotes(anyString(), any(UUID.class), any(LocalDate.class), any(LocalDate.class)))
                .thenThrow(new RuntimeException("API unavailable"));
        when(jobRunService.save(any(ImportJobRun.class))).thenReturn(jobRun);

        // Act
        importService.runImport();

        // Assert
        verify(reportService).logError(
                eq(jobRun),
                isNull(),
                isNull(),
                eq(activeClient.getGuid()),
                contains("Failed to process client notes"),
                any(RuntimeException.class)
        );
    }

    @Test
    void runImport_WhenGetAllClientsFails_ShouldThrowImportException() {
        // Arrange
        when(oldSystemClientService.getAllClients())
                .thenThrow(new RuntimeException("Old system unavailable"));
        when(jobRunService.save(any(ImportJobRun.class))).thenReturn(jobRun);

        // Act & Assert
        assertThrows(ImportException.class, () -> importService.runImport());

        verify(reportService).logError(
                any(ImportJobRun.class),
                isNull(),
                isNull(),
                isNull(),
                contains("Fatal error"),
                any(RuntimeException.class)
        );
    }

    @Test
    void runImport_ShouldUpdateStatisticsCorrectly() {
        // Arrange
        OldNoteDto note1 = OldNoteDto.builder().guid(UUID.randomUUID()).comments("Note 1").build();
        OldNoteDto note2 = OldNoteDto.builder().guid(UUID.randomUUID()).comments("Note 2").build();
        OldNoteDto note3 = OldNoteDto.builder().guid(UUID.randomUUID()).comments("Note 3").build();

        when(props.getLookbackYears()).thenReturn(1);
        when(oldSystemClientService.getAllClients()).thenReturn(List.of(activeClient));
        when(patientService.savePatients(anyList())).thenReturn(List.of(patientProfile));
        when(patientService.buildGuidToPatientMap()).thenReturn(Map.of(activeClient.getGuid(), patientProfile));
        when(oldSystemClientService.getClientNotes(anyString(), any(UUID.class), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(note1, note2, note3));
        when(noteService.upsertNote(any(PatientProfile.class), any(OldNoteDto.class)))
                .thenReturn(
                        NoteServiceImpl.ImportResult.CREATED,
                        NoteServiceImpl.ImportResult.UPDATED,
                        NoteServiceImpl.ImportResult.SKIPPED
                );
        when(jobRunService.save(any(ImportJobRun.class))).thenReturn(jobRun);

        // Act
        importService.runImport();

        // Assert
        verify(jobRunService, times(2)).save(jobRunCaptor.capture());
        ImportJobRun completedJob = jobRunCaptor.getAllValues().get(1);

        assertEquals(1, completedJob.getNewCount()); // CREATED
        assertEquals(1, completedJob.getUpdatedCount()); // UPDATED
        assertEquals(1, completedJob.getSkippedCount()); // SKIPPED
        assertEquals(0, completedJob.getErrorCount()); // No errors
    }

    @Test
    void isClientActive_ShouldReturnTrueForActiveStatuses() throws Exception {
        Method method = ImportServiceImpl.class.getDeclaredMethod("isClientActive", OldClientDto.class);
        method.setAccessible(true);

        assertTrue((Boolean) method.invoke(importService, OldClientDto.builder().status((short) 210).build()));
        assertTrue((Boolean) method.invoke(importService, OldClientDto.builder().status((short) 220).build()));
        assertTrue((Boolean) method.invoke(importService, OldClientDto.builder().status((short) 230).build()));
    }


    @Test
    void isClientActive_ShouldReturnFalseForInactiveStatuses() throws Exception {
        Method method = ImportServiceImpl.class.getDeclaredMethod("isClientActive", OldClientDto.class);
        method.setAccessible(true);

        assertFalse((Boolean) method.invoke(importService, OldClientDto.builder().status((short) 100).build()));
        assertFalse((Boolean) method.invoke(importService, OldClientDto.builder().status((short) 500).build()));
    }

    @Test
    void isClientActive_ShouldReturnFalseForNullStatus() throws Exception {
        Method method = ImportServiceImpl.class.getDeclaredMethod("isClientActive", OldClientDto.class);
        method.setAccessible(true);

        assertFalse((Boolean) method.invoke(importService, OldClientDto.builder().status(null).build()));
    }

    @Test
    void runImport_ShouldUseCorrectDateRange() {
        // Arrange
        LocalDate expectedTo = LocalDate.now();
        LocalDate expectedFrom = expectedTo.minusYears(1);

        when(props.getLookbackYears()).thenReturn(1);
        when(oldSystemClientService.getAllClients()).thenReturn(List.of(activeClient));
        when(patientService.savePatients(anyList())).thenReturn(List.of(patientProfile));
        when(patientService.buildGuidToPatientMap()).thenReturn(Map.of(activeClient.getGuid(), patientProfile));
        when(oldSystemClientService.getClientNotes(anyString(), any(UUID.class), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(jobRunService.save(any(ImportJobRun.class))).thenReturn(jobRun);

        // Act
        importService.runImport();

        // Assert
        verify(oldSystemClientService).getClientNotes(
                eq(activeClient.getAgency()),
                eq(activeClient.getGuid()),
                eq(expectedFrom),
                eq(expectedTo)
        );
    }
}