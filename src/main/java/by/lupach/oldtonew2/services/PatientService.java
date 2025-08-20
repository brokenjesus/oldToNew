package by.lupach.oldtonew2.services;

import by.lupach.oldtonew2.dtos.OldClientDto;
import by.lupach.oldtonew2.entities.OldClientGuid;
import by.lupach.oldtonew2.entities.PatientProfile;
import by.lupach.oldtonew2.exceptions.InvalidDataException;
import by.lupach.oldtonew2.exceptions.PatientCreationException;
import by.lupach.oldtonew2.repositories.PatientProfileRepository;
import by.lupach.oldtonew2.utils.DateParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class PatientService {

    private final PatientProfileRepository patientRepo;

    public Map<String, PatientProfile> buildGuidToPatientMap() {
        Map<String, PatientProfile> map = new HashMap<>();
        List<PatientProfile> allPatients = patientRepo.findAll();

        for (PatientProfile patient : allPatients) {
            for (OldClientGuid guid : patient.getOldClientGuids()) {
                map.put(guid.getGuid().toString(), patient);
            }
        }
        return map;
    }

    public PatientProfile createAndSavePatientFromOldClient(OldClientDto oldClient) {
        validateOldClient(oldClient);

        try {
            PatientProfile newPatient = PatientProfile.builder()
                    .firstName(oldClient.getFirstName())
                    .lastName(oldClient.getLastName())
                    .statusId(oldClient.getStatus())
                    .oldClientGuids(new ArrayList<>())
                    .build();

            OldClientGuid clientGuid = OldClientGuid.builder()
                    .guid(UUID.fromString(oldClient.getGuid()))
                    .build();
            newPatient.getOldClientGuids().add(clientGuid);

            return patientRepo.save(newPatient);
        } catch (Exception e) {
            String errorMsg = String.format("Failed to create patient from old client: %s", oldClient.getGuid());
            log.error(errorMsg, e);
            throw new PatientCreationException(errorMsg, e);
        }
    }
    public List<PatientProfile> savePatients(List<OldClientDto> oldClients) {
        if (oldClients == null || oldClients.isEmpty()) {
            return Collections.emptyList();
        }

        // Шаг 1: Находим дубликаты
        Map<String, List<OldClientDto>> groupedPatients = groupPatientsByKey(oldClients);

        // Шаг 2: Обрабатываем каждую группу
        List<PatientProfile> resultPatients = new ArrayList<>();

        for (List<OldClientDto> group : groupedPatients.values()) {
            if (group.size() == 1) {
                // Уникальный пациент - создаем нового
                resultPatients.add(createAndSavePatientFromOldClient(group.get(0)));
            } else {
                // Дубликаты - мержим и создаем одного пациента
                resultPatients.add(mergeAndCreatePatient(group));
            }
        }

        return resultPatients;
    }

    private Map<String, List<OldClientDto>> groupPatientsByKey(List<OldClientDto> oldClients) {
        return oldClients.stream()
                .filter(client -> isValidForGrouping(client))
                .collect(Collectors.groupingBy(
                        client -> createPatientKey(client.getFirstName(), client.getLastName(), client.getDob())
                ));
    }

    private String createPatientKey(String firstName, String lastName, String dob) {
        return (firstName != null ? firstName.toLowerCase().trim() : "") +
                (lastName != null ? lastName.toLowerCase().trim() : "") +
                (dob != null ? dob.trim() : "");
    }

    private boolean isValidForGrouping(OldClientDto client) {
        return client != null &&
                client.getFirstName() != null &&
                client.getLastName() != null &&
                client.getDob() != null;
    }

    private PatientProfile mergeAndCreatePatient(List<OldClientDto> duplicates) {
        // Выбираем основного пациента по приоритету
        OldClientDto mainPatient = selectMainPatient(duplicates);

        // Создаем пациента с основными данными
        PatientProfile patient = createAndSavePatientFromOldClient(mainPatient);

        // Добавляем GUID всех дубликатов
        for (OldClientDto duplicate : duplicates) {
            if (!duplicate.getGuid().equals(mainPatient.getGuid())) {
                OldClientGuid additionalGuid = OldClientGuid.builder()
                        .guid(UUID.fromString(duplicate.getGuid()))
                        .build();
                patient.getOldClientGuids().add(additionalGuid);
            }
        }

        return patientRepo.save(patient);
    }

    private OldClientDto selectMainPatient(List<OldClientDto> patients) {
        // Ищем пациента с приоритетным статусом
        Optional<OldClientDto> priorityPatient = patients.stream()
                .filter(client -> client.getStatus() == 210 ||
                        client.getStatus() == 220 ||
                        client.getStatus() == 230)
                .findFirst();

        if (priorityPatient.isPresent()) {
            return priorityPatient.get();
        }

        // Если нет приоритетного статуса, выбираем с последней датой создания
        return patients.stream()
                .max(Comparator.comparing(client -> DateParser.parseOldSystemTimestamp(client.getCreatedDateTime())))
                .orElse(patients.get(0));
    }

    private void validateOldClient(OldClientDto oldClient) {
        if (oldClient == null) {
            throw new InvalidDataException("Old client data is null");
        }

        if (StringUtils.isBlank(oldClient.getGuid())) {
            throw new InvalidDataException("Client GUID is blank");
        }

        try {
            UUID.fromString(oldClient.getGuid());
        } catch (IllegalArgumentException e) {
            throw new InvalidDataException("Invalid GUID format: " + oldClient.getGuid());
        }
    }
}