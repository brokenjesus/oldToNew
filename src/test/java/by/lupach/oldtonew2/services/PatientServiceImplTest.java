package by.lupach.oldtonew2.services;

import by.lupach.oldtonew2.dtos.OldClientDto;
import by.lupach.oldtonew2.entities.OldClientGuid;
import by.lupach.oldtonew2.entities.PatientProfile;
import by.lupach.oldtonew2.exceptions.InvalidDataException;
import by.lupach.oldtonew2.repositories.PatientProfileRepository;
import by.lupach.oldtonew2.utils.DateParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientServiceImplTest {

    @Mock
    private PatientProfileRepository patientRepo;

    @Mock
    private DateParser dateParser;

    @InjectMocks
    private PatientServiceImpl patientService;

    private OldClientDto client1;
    private OldClientDto client2;
    private OldClientDto client3;

    @BeforeEach
    void setUp() {
        client1 = OldClientDto.builder()
                .guid(UUID.randomUUID())
                .firstName("John")
                .lastName("Doe")
                .dob("1990-01-01")
                .status((short) 210)
                .createdDateTime("2023-01-01T10:00:00")
                .build();

        client2 = OldClientDto.builder()
                .guid(UUID.randomUUID())
                .firstName("John")
                .lastName("Doe")
                .dob("1990-01-01")
                .status((short) 220)
                .createdDateTime("2023-01-02T10:00:00")
                .build();

        client3 = OldClientDto.builder()
                .guid(UUID.randomUUID())
                .firstName("Jane")
                .lastName("Smith")
                .dob("1985-05-15")
                .status((short) 100)
                .createdDateTime("2023-01-03T10:00:00")
                .build();
    }

    @Test
    void savePatients_UniquePatients() {
        List<OldClientDto> clients = Arrays.asList(client1, client3);
        PatientProfile patient1 = createPatientFromClient(client1);
        PatientProfile patient3 = createPatientFromClient(client3);

        when(patientRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<PatientProfile> result = patientService.savePatients(clients);

        assertEquals(2, result.size());
        verify(patientRepo, times(2)).save(any());
    }

    @Test
    void savePatients_DuplicatePatients() {
        List<OldClientDto> clients = Arrays.asList(client1, client2); // Дубликаты
        PatientProfile mergedPatient = createMergedPatient(Arrays.asList(client1, client2));

        when(patientRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<PatientProfile> result = patientService.savePatients(clients);

        assertEquals(1, result.size());
        PatientProfile resultPatient = result.get(0);
        assertEquals(2, resultPatient.getOldClientGuids().size());
    }

    @Test
    void savePatients_EmptyList() {
        List<PatientProfile> result = patientService.savePatients(Collections.emptyList());

        assertTrue(result.isEmpty());
        verify(patientRepo, never()).save(any());
    }

    @Test
    void buildGuidToPatientMap() {
        PatientProfile patient = createPatientFromClient(client1);
        patient.getOldClientGuids().add(OldClientGuid.builder().guid(client2.getGuid()).build());

        when(patientRepo.findAll()).thenReturn(List.of(patient));

        Map<UUID, PatientProfile> result = patientService.buildGuidToPatientMap();

        assertEquals(2, result.size());
        assertTrue(result.containsKey(client1.getGuid()));
        assertTrue(result.containsKey(client2.getGuid()));
    }

    @Test
    void createPatientFromOldClient_ValidData() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        when(patientRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Method method = PatientServiceImpl.class.getDeclaredMethod(
                "createAndSavePatientFromOldClient",
                OldClientDto.class
        );
        method.setAccessible(true); // Make the private method accessible

        PatientProfile result = (PatientProfile) method.invoke(patientService, client1);

        assertNotNull(result);
        assertEquals("John", result.getFirstName());
        assertEquals("Doe", result.getLastName());
        assertEquals(1, result.getOldClientGuids().size());
        assertEquals(client1.getGuid(), result.getOldClientGuids().get(0).getGuid());
    }

    @Test
    void selectMainPatient_PriorityStatus() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        List<OldClientDto> patients = Arrays.asList(
                OldClientDto.builder().guid(UUID.randomUUID()).status((short) 100).createdDateTime("2023-01-01T10:00:00").build(),
                OldClientDto.builder().guid(UUID.randomUUID()).status((short) 210).createdDateTime("2023-01-02T10:00:00").build(),
                OldClientDto.builder().guid(UUID.randomUUID()).status((short) 100).createdDateTime("2023-01-03T10:00:00").build()
        );

        Method method = PatientServiceImpl.class.getDeclaredMethod("selectMainPatient", List.class);
        method.setAccessible(true);

        OldClientDto result = (OldClientDto) method.invoke(patientService, patients);

        assertEquals((short) 210, result.getStatus());
    }

    @Test
    void selectMainPatient_ByCreationDate() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        List<OldClientDto> patients = Arrays.asList(
                OldClientDto.builder().guid(UUID.randomUUID()).status((short) 100).createdDateTime("2023-01-01T10:00:00").build(),
                OldClientDto.builder().guid(UUID.randomUUID()).status((short) 100).createdDateTime("2023-01-03T10:00:00").build(),
                OldClientDto.builder().guid(UUID.randomUUID()).status((short) 100).createdDateTime("2023-01-02T10:00:00").build()
        );

        when(dateParser.parseOldSystemTimestamp("2023-01-01T10:00:00")).thenReturn(LocalDateTime.of(2023, 1, 1, 10, 0));
        when(dateParser.parseOldSystemTimestamp("2023-01-02T10:00:00")).thenReturn(LocalDateTime.of(2023, 1, 2, 10, 0));
        when(dateParser.parseOldSystemTimestamp("2023-01-03T10:00:00")).thenReturn(LocalDateTime.of(2023, 1, 3, 10, 0));


        Method method = PatientServiceImpl.class.getDeclaredMethod("selectMainPatient", List.class);
        method.setAccessible(true);
        OldClientDto result = (OldClientDto) method.invoke(patientService, patients);

        assertEquals(patients.get(1).getGuid(), result.getGuid());
    }

    private PatientProfile createPatientFromClient(OldClientDto client) {
        return PatientProfile.builder()
                .firstName(client.getFirstName())
                .lastName(client.getLastName())
                .oldClientGuids(new ArrayList<>(List.of(
                        OldClientGuid.builder().guid(client.getGuid()).build()
                )))
                .build();
    }

    private PatientProfile createMergedPatient(List<OldClientDto> clients) {
        PatientProfile patient = createPatientFromClient(clients.get(0));
        for (int i = 1; i < clients.size(); i++) {
            patient.getOldClientGuids().add(OldClientGuid.builder().guid(clients.get(i).getGuid()).build());
        }
        return patient;
    }
}