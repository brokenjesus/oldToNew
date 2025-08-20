package by.lupach.oldtonew.services;

import by.lupach.oldtonew.dtos.OldClientDto;
import by.lupach.oldtonew.entities.OldClientGuid;
import by.lupach.oldtonew.entities.PatientProfile;
import by.lupach.oldtonew.models.ImportStatistics;
import by.lupach.oldtonew.repositories.PatientProfileRepository;
import by.lupach.oldtonew.utils.DateParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientServiceImplTest {

    @Mock
    private PatientProfileRepository patientRepo;

    @Mock
    private OldClientGuidService oldClientGuidService;

    @Mock
    private DateParser dateParser;

    @Mock
    private ImportStatistics statistics;

    @InjectMocks
    private PatientServiceImpl patientService;

    private OldClientDto testClient1;
    private OldClientDto testClient2;
    private OldClientDto testClient3;
    private OldClientGuid existingGuid;
    private PatientProfile existingPatient;

    @BeforeEach
    void setUp() {
        statistics = mock(ImportStatistics.class);
        try {
            Field statisticsField = PatientServiceImpl.class.getDeclaredField("statistics");
            statisticsField.setAccessible(true);
            statisticsField.set(patientService, statistics);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set statistics field", e);
        }

        testClient1 = OldClientDto.builder()
                .guid(UUID.randomUUID())
                .firstName("John")
                .lastName("Doe")
                .dob("1990-01-01")
                .status((short)210)
                .createdDateTime("2023-01-01T10:00:00")
                .build();

        testClient2 = OldClientDto.builder()
                .guid(UUID.randomUUID())
                .firstName("John")
                .lastName("Doe")
                .dob("1990-01-01")
                .status((short)200)
                .createdDateTime("2023-01-02T10:00:00")
                .build();

        testClient3 = OldClientDto.builder()
                .guid(UUID.randomUUID())
                .firstName("Jane")
                .lastName("Smith")
                .dob("1985-05-15")
                .status((short)220)
                .createdDateTime("2023-01-03T10:00:00")
                .build();

        existingGuid = OldClientGuid.builder()
                .guid(UUID.randomUUID())
                .patientProfileId(1L)
                .build();

        existingPatient = PatientProfile.builder()
                .id(1L)
                .firstName("Existing")
                .lastName("Patient")
                .oldClientGuids(new ArrayList<>())
                .build();
    }

    @Test
    void buildGuidToPatientMap_ShouldReturnCorrectMap() {
        List<PatientProfile> patients = Arrays.asList(
                PatientProfile.builder()
                        .id(1L)
                        .oldClientGuids(Arrays.asList(
                                OldClientGuid.builder().guid(UUID.randomUUID()).build(),
                                OldClientGuid.builder().guid(UUID.randomUUID()).build()
                        ))
                        .build(),
                PatientProfile.builder()
                        .id(2L)
                        .oldClientGuids(Arrays.asList(
                                OldClientGuid.builder().guid(UUID.randomUUID()).build()
                        ))
                        .build()
        );

        when(patientRepo.findAll()).thenReturn(patients);

        Map<UUID, PatientProfile> result = patientService.buildGuidToPatientMap();

        assertEquals(3, result.size());
        patients.forEach(patient ->
                patient.getOldClientGuids().forEach(guid ->
                        assertEquals(patient, result.get(guid.getGuid()))
                )
        );
    }

    @Test
    void savePatients_WithNullList_ShouldReturnEmptyList() {
        List<PatientProfile> result = patientService.savePatients(null);

        assertTrue(result.isEmpty());
        verify(statistics, never()).incrementSkippedPatients(anyInt());
        verify(statistics, never()).incrementUpdatedPatients(anyInt());
        verify(statistics, never()).incrementNewPatients(anyInt());
    }

    @Test
    void savePatients_WithEmptyList_ShouldReturnEmptyList() {
        List<PatientProfile> result = patientService.savePatients(Collections.emptyList());

        assertTrue(result.isEmpty());
        verify(statistics, never()).incrementSkippedPatients(anyInt());
        verify(statistics, never()).incrementUpdatedPatients(anyInt());
        verify(statistics, never()).incrementNewPatients(anyInt());
    }

    @Test
    void savePatients_WithNewUniquePatient_ShouldCreatePatient() throws Exception {
        List<OldClientDto> clients = Arrays.asList(testClient1);
        List<OldClientGuid> existingGuids = Collections.emptyList();
        PatientProfile savedPatient = PatientProfile.builder()
                .id(1L)
                .firstName(testClient1.getFirstName())
                .lastName(testClient1.getLastName())
                .statusId(testClient1.getStatus())
                .oldClientGuids(new ArrayList<>())
                .build();

        when(oldClientGuidService.getAllClientGuids()).thenReturn(existingGuids);
        when(patientRepo.save(any(PatientProfile.class))).thenReturn(savedPatient);
        when(statistics.getUpdatedPatients()).thenReturn(0);

        List<PatientProfile> result = patientService.savePatients(clients);

        assertEquals(1, result.size());
        verify(statistics).incrementSkippedPatients(1);
        verify(statistics).incrementUpdatedPatients(0);
        verify(statistics).incrementNewPatients(1);
    }

    @Test
    void savePatients_WithDuplicatePatients_ShouldMergeAndCreateOne() throws Exception {
        List<OldClientDto> clients = Arrays.asList(testClient1, testClient2);
        List<OldClientGuid> existingGuids = Collections.emptyList();
        PatientProfile savedPatient = PatientProfile.builder()
                .id(1L)
                .firstName(testClient1.getFirstName())
                .lastName(testClient1.getLastName())
                .statusId(testClient1.getStatus())
                .oldClientGuids(new ArrayList<>())
                .build();

        when(oldClientGuidService.getAllClientGuids()).thenReturn(existingGuids);
        when(patientRepo.save(any(PatientProfile.class))).thenReturn(savedPatient);

        List<PatientProfile> result = patientService.savePatients(clients);

        assertEquals(1, result.size());
        verify(statistics).incrementSkippedPatients(1);
        verify(statistics).incrementNewPatients(1);
    }

    @Test
    void savePatients_WithMixedExistingAndNewGuids_ShouldUpdatePatient() throws Exception {
        List<OldClientDto> clients = Arrays.asList(testClient1, testClient2);
        List<OldClientGuid> existingGuids = Arrays.asList(
                OldClientGuid.builder().guid(testClient1.getGuid()).patientProfileId(1L).build()
        );

        when(oldClientGuidService.getAllClientGuids()).thenReturn(existingGuids);
        when(oldClientGuidService.getByGuid(testClient1.getGuid()))
                .thenReturn(Optional.of(existingGuids.get(0)));
        when(patientRepo.getReferenceById(1L)).thenReturn(existingPatient);
        when(patientRepo.save(any(PatientProfile.class))).thenReturn(existingPatient);
        lenient().when(dateParser.parseOldSystemTimestamp(anyString())).thenReturn(LocalDateTime.now());
        when(statistics.getUpdatedPatients()).thenReturn(1);

        List<PatientProfile> result = patientService.savePatients(clients);

        assertEquals(1, result.size());
        verify(statistics).incrementSkippedPatients(0);
        verify(statistics).incrementUpdatedPatients(1);
        verify(statistics).incrementNewPatients(0);
    }

    @Test
    void save_WithNullClient_ShouldThrowInvalidDataException() throws Exception {
        Method method = PatientServiceImpl.class.getDeclaredMethod("save", OldClientDto.class);
        method.setAccessible(true);

        assertThrows(InvocationTargetException.class, () ->
                method.invoke(patientService, (Object) null)
        );
    }

    @Test
    void save_WithValidClient_ShouldCreatePatient() throws Exception {
        when(patientRepo.save(any(PatientProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Method method = PatientServiceImpl.class.getDeclaredMethod("save", OldClientDto.class);
        method.setAccessible(true);

        PatientProfile result = (PatientProfile) method.invoke(patientService, testClient1);

        assertNotNull(result);
        assertEquals(testClient1.getFirstName(), result.getFirstName());
        assertEquals(testClient1.getLastName(), result.getLastName());
        assertEquals(testClient1.getStatus(), result.getStatusId());
        assertEquals(1, result.getOldClientGuids().size());
        assertEquals(testClient1.getGuid(), result.getOldClientGuids().get(0).getGuid());
    }

    @Test
    void save_WithDatabaseError_ShouldThrowPatientCreationException() throws Exception {
        when(patientRepo.save(any(PatientProfile.class))).thenThrow(new RuntimeException("DB error"));

        Method method = PatientServiceImpl.class.getDeclaredMethod("save", OldClientDto.class);
        method.setAccessible(true);

        assertThrows(InvocationTargetException.class, () ->
                method.invoke(patientService, testClient1)
        );
    }

    @Test
    void groupPatientsByKey_ShouldGroupCorrectly() throws Exception {
        List<OldClientDto> clients = Arrays.asList(testClient1, testClient2, testClient3);

        Method method = PatientServiceImpl.class.getDeclaredMethod("groupPatientsByKey", List.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<String, List<OldClientDto>> result = (Map<String, List<OldClientDto>>) method.invoke(patientService, clients);

        assertEquals(2, result.size());

        String key1 = "johndoe1990-01-01";
        String key2 = "janesmith1985-05-15";

        assertTrue(result.containsKey(key1));
        assertTrue(result.containsKey(key2));
        assertEquals(2, result.get(key1).size());
        assertEquals(1, result.get(key2).size());
    }

    @Test
    void groupPatientsByKey_WithInvalidClients_ShouldFilterThemOut() throws Exception {
        OldClientDto invalidClient = OldClientDto.builder()
                .guid(UUID.randomUUID())
                .firstName(null)
                .lastName("Doe")
                .dob("1990-01-01")
                .build();

        List<OldClientDto> clients = Arrays.asList(testClient1, invalidClient);

        Method method = PatientServiceImpl.class.getDeclaredMethod("groupPatientsByKey", List.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        Map<String, List<OldClientDto>> result = (Map<String, List<OldClientDto>>) method.invoke(patientService, clients);

        assertEquals(1, result.size());
        assertEquals(1, result.values().iterator().next().size());
    }

    @Test
    void selectMainPatient_WithPriorityStatus_ShouldSelectPriority() throws Exception {
        List<OldClientDto> clients = Arrays.asList(testClient1, testClient2, testClient3);

        Method method = PatientServiceImpl.class.getDeclaredMethod("selectMainPatient", List.class);
        method.setAccessible(true);

        OldClientDto result = (OldClientDto) method.invoke(patientService, clients);

        assertNotNull(result);
        assertTrue(result.getStatus() == 210 || result.getStatus() == 220 || result.getStatus() == 230);
    }

    @Test
    void selectMainPatient_WithoutPriorityStatus_ShouldSelectLatest() throws Exception {
        OldClientDto client1 = OldClientDto.builder()
                .guid(UUID.randomUUID())
                .firstName("John")
                .lastName("Doe")
                .dob("1990-01-01")
                .status((short)200)
                .createdDateTime("2023-01-01T10:00:00")
                .build();

        OldClientDto client2 = OldClientDto.builder()
                .guid(UUID.randomUUID())
                .firstName("John")
                .lastName("Doe")
                .dob("1990-01-01")
                .status((short)200)
                .createdDateTime("2023-01-02T10:00:00")
                .build();

        List<OldClientDto> clients = Arrays.asList(client1, client2);
        LocalDateTime date1 = LocalDateTime.of(2023, 1, 1, 10, 0, 0);
        LocalDateTime date2 = LocalDateTime.of(2023, 1, 2, 10, 0, 0);

        when(dateParser.parseOldSystemTimestamp("2023-01-01T10:00:00")).thenReturn(date1);
        when(dateParser.parseOldSystemTimestamp("2023-01-02T10:00:00")).thenReturn(date2);

        Method method = PatientServiceImpl.class.getDeclaredMethod("selectMainPatient", List.class);
        method.setAccessible(true);

        OldClientDto result = (OldClientDto) method.invoke(patientService, clients);

        assertNotNull(result);
        assertEquals(client2.getGuid(), result.getGuid());
    }

    @Test
    void mergeAndUpdate_WithExistingPatient_ShouldUpdateWithAdditionalGuids() throws Exception {
        List<OldClientDto> duplicates = Arrays.asList(testClient1, testClient2);
        OldClientGuid existingGuid = OldClientGuid.builder()
                .guid(testClient1.getGuid())
                .patientProfileId(1L)
                .build();

        when(oldClientGuidService.getByGuid(testClient1.getGuid()))
                .thenReturn(Optional.of(existingGuid));
        when(patientRepo.getReferenceById(1L)).thenReturn(existingPatient);
        when(patientRepo.save(any(PatientProfile.class))).thenReturn(existingPatient);

        Method method = PatientServiceImpl.class.getDeclaredMethod("mergeAndUpdate", List.class);
        method.setAccessible(true);

        PatientProfile result = (PatientProfile) method.invoke(patientService, duplicates);

        assertNotNull(result);
        assertEquals(1, result.getOldClientGuids().size());
        verify(patientRepo).save(existingPatient);
    }

    @Test
    void mergeAndUpdate_WithoutExistingGuid_ShouldThrowInvalidDataException() throws Exception {
        List<OldClientDto> duplicates = Arrays.asList(testClient1, testClient2);

        when(oldClientGuidService.getByGuid(any(UUID.class))).thenReturn(Optional.empty());

        Method method = PatientServiceImpl.class.getDeclaredMethod("mergeAndUpdate", List.class);
        method.setAccessible(true);

        assertThrows(InvocationTargetException.class, () ->
                method.invoke(patientService, duplicates)
        );
    }

    @Test
    void getByIdInNewSystem_ShouldReturnPatient() {
        when(patientRepo.getReferenceById(1L)).thenReturn(existingPatient);

        PatientProfile result = patientService.getByIdInNewSystem(1L);

        assertNotNull(result);
        assertEquals(existingPatient, result);
    }

    @Test
    void isValidForGrouping_WithValidClient_ShouldReturnTrue() throws Exception {
        Method method = PatientServiceImpl.class.getDeclaredMethod("isValidForGrouping", OldClientDto.class);
        method.setAccessible(true);

        boolean result = (Boolean) method.invoke(patientService, testClient1);

        assertTrue(result);
    }

    @Test
    void isValidForGrouping_WithNullClient_ShouldReturnFalse() throws Exception {
        Method method = PatientServiceImpl.class.getDeclaredMethod("isValidForGrouping", OldClientDto.class);
        method.setAccessible(true);

        boolean result = (Boolean) method.invoke(patientService, (Object) null);

        assertFalse(result);
    }

    @Test
    void isValidForGrouping_WithMissingFirstName_ShouldReturnFalse() throws Exception {
        OldClientDto invalidClient = OldClientDto.builder()
                .guid(UUID.randomUUID())
                .firstName(null)
                .lastName("Doe")
                .dob("1990-01-01")
                .build();

        Method method = PatientServiceImpl.class.getDeclaredMethod("isValidForGrouping", OldClientDto.class);
        method.setAccessible(true);

        boolean result = (Boolean) method.invoke(patientService, invalidClient);

        assertFalse(result);
    }

    @Test
    void createPatientKey_ShouldCreateConsistentKey() throws Exception {
        Method method = PatientServiceImpl.class.getDeclaredMethod("createPatientKey", String.class, String.class, String.class);
        method.setAccessible(true);

        String key1 = (String) method.invoke(patientService, "John", "Doe", "1990-01-01");
        String key2 = (String) method.invoke(patientService, "JOHN", "DOE", "1990-01-01");
        String key3 = (String) method.invoke(patientService, " John ", " Doe ", " 1990-01-01 ");

        assertEquals("johndoe1990-01-01", key1);
        assertEquals("johndoe1990-01-01", key2);
        assertEquals("johndoe1990-01-01", key3);
    }

    @Test
    void validateOldClient_WithValidClient_ShouldNotThrow() throws Exception {
        Method method = PatientServiceImpl.class.getDeclaredMethod("validateOldClient", OldClientDto.class);
        method.setAccessible(true);

        assertDoesNotThrow(() -> method.invoke(patientService, testClient1));
    }

    @Test
    void validateOldClient_WithNullClient_ShouldThrow() throws Exception {
        Method method = PatientServiceImpl.class.getDeclaredMethod("validateOldClient", OldClientDto.class);
        method.setAccessible(true);

        assertThrows(InvocationTargetException.class, () ->
                method.invoke(patientService, (Object) null)
        );
    }
}