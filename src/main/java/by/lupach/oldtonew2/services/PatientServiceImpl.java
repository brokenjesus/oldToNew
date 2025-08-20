package by.lupach.oldtonew2.services;

import by.lupach.oldtonew2.dtos.OldClientDto;
import by.lupach.oldtonew2.entities.OldClientGuid;
import by.lupach.oldtonew2.entities.PatientProfile;
import by.lupach.oldtonew2.exceptions.InvalidDataException;
import by.lupach.oldtonew2.exceptions.PatientCreationException;
import by.lupach.oldtonew2.models.ImportStatistics;
import by.lupach.oldtonew2.repositories.PatientProfileRepository;
import by.lupach.oldtonew2.utils.DateParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class PatientServiceImpl implements PatientService {

    private final PatientProfileRepository patientRepo;
    private final OldClientGuidService oldClientGuidService;
    private final DateParser dateParser;

    @Autowired
    private ImportStatistics statistics;


    @Override
    public Map<UUID, PatientProfile> buildGuidToPatientMap() {
        Map<UUID, PatientProfile> map = new HashMap<>();
        List<PatientProfile> allPatients = patientRepo.findAll();

        for (PatientProfile patient : allPatients) {
            for (OldClientGuid guid : patient.getOldClientGuids()) {
                map.put(guid.getGuid(), patient);
            }
        }
        return map;
    }

    @Override
    public List<PatientProfile> savePatients(List<OldClientDto> oldClients) {
        if (oldClients == null || oldClients.isEmpty()) {
            return Collections.emptyList();
        }

        // Шаг 1: Проверяем существующие GUID
//        List<UUID> oldClientsGuids = oldClients.stream().map(OldClientDto::getGuid).toList();
        List<OldClientGuid> existingOldClientGuids = oldClientGuidService.getAllClientGuids();


        // Шаг 3: Находим дубликаты (если ФИО + дата рождения совпадает)
        // из тз следует, что 1 пациенту соответвует 1 или более клиентов в старой системе,
        // следовательно существуют дубликаты пользователей, но с разным guid
        // т.к. у нас не храниться номер паспорта, или еще какое-то поле по которому можно однозначно сказать кто дубликат
        // дубликаты находятся по ФИО + дата рождения
        Map<String, List<OldClientDto>> oldGroupedPatients = groupPatientsByKey(oldClients);

//        System.out.println(groupedPatients.size());

        // Шаг 2: Фильтруем клиентов, оставляя только те, чьи GUID еще не существуют

        Map<String, List<OldClientDto>> newGroupedPatients = oldGroupedPatients.entrySet().stream()
                .filter(entry -> entry.getValue().stream()
                        .noneMatch(client ->
                                existingOldClientGuids.stream().map(OldClientGuid::getGuid)
                                        .toList()
                                        .contains(client.getGuid()))
                )
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        oldGroupedPatients.keySet().removeIf(newGroupedPatients::containsKey);

        //дубликат мог появиться в старой системе при обновлении, а наш пациент не ссылается на те guid, следовательно,
        // заметки могут быть утеряны, поэтому необходимо добавить недосотоющий guid пациенту
        Map<String, List<OldClientDto>> oldGroupedPatientsToUpdate = oldGroupedPatients.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1) // только дубликаты
                .filter(entry -> entry.getValue().stream()
                        .anyMatch(client -> !existingOldClientGuids.stream().map(OldClientGuid::getGuid)
                                .toList()
                                .contains(client.getGuid())) // есть хотя бы один новый
                )
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // Шаг 4: Обрабатываем каждую группу
        List<PatientProfile> resultPatients = new ArrayList<>();

        for (List<OldClientDto> group : oldGroupedPatientsToUpdate.values()) {
            {
                // Дубликаты - мержим и обновляем пациента
                resultPatients.add(mergeAndUpdate(group));
            }
        }


        statistics.incrementSkippedPatients(oldGroupedPatients.size()-resultPatients.size());


        for (List<OldClientDto> group : newGroupedPatients.values()) {
            if (group.size() == 1) {
                // Уникальный пациент - создаем нового
                resultPatients.add(save(group.get(0)));
            } else {
                // Дубликаты - мержим и создаем одного пациента
                resultPatients.add(mergeAndSave(group));
            }
        }

        statistics.incrementNewPatients(resultPatients.size());
        return resultPatients;
    }

    private PatientProfile mergeAndUpdate(List<OldClientDto> duplicates) {
        //ищем id пациента в новой системе по таблице соотносящей guid с id в новой
        Long idInNewSystem = duplicates.stream()
                .map(OldClientDto::getGuid)
                .map(oldClientGuidService::getByGuid)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(OldClientGuid::getPatientProfileId)
                .findFirst()
                .orElse(null); // null if none found

        if (idInNewSystem == null) {
            throw new InvalidDataException("No patient was found found");
        }

        PatientProfile patientProfile = getByIdInNewSystem(idInNewSystem);

        OldClientDto mainPatient = selectMainPatient(duplicates);
        patientProfile.setStatusId(mainPatient.getStatus());

        for (OldClientDto duplicate : duplicates) {
            if (!duplicate.getGuid().equals(mainPatient.getGuid())) {
                OldClientGuid additionalGuid = OldClientGuid.builder()
                        .guid(duplicate.getGuid())
                        .build();
                patientProfile.getOldClientGuids().add(additionalGuid);
            }
        }

        return patientRepo.save(patientProfile);
    }


    public PatientProfile getByIdInNewSystem(Long id) {
        return patientRepo.getReferenceById(id);
    }

    private PatientProfile save(OldClientDto oldClient) {
        validateOldClient(oldClient);

        try {
            PatientProfile newPatient = PatientProfile.builder()
                    .firstName(oldClient.getFirstName())
                    .lastName(oldClient.getLastName())
                    .statusId(oldClient.getStatus())
                    .oldClientGuids(new ArrayList<>())
                    .build();

            OldClientGuid clientGuid = OldClientGuid.builder()
                    .guid(oldClient.getGuid())
                    .build();
            newPatient.getOldClientGuids().add(clientGuid);

            return patientRepo.save(newPatient);
        } catch (Exception e) {
            String errorMsg = String.format("Failed to create patient from old client: %s", oldClient.getGuid());
            log.error(errorMsg, e);
            throw new PatientCreationException(errorMsg, e);
        }
    }

    private Map<String, List<OldClientDto>> groupPatientsByKey(List<OldClientDto> oldClients) {
        return oldClients.stream()
                .filter(this::isValidForGrouping)
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

    private PatientProfile mergeAndSave(List<OldClientDto> duplicates) {
        // Выбираем основного пациента по приоритету
        OldClientDto mainPatient = selectMainPatient(duplicates);

        // Создаем пациента с основными данными
        PatientProfile patient = save(mainPatient);

        // Добавляем GUID всех дубликатов
        for (OldClientDto duplicate : duplicates) {
            if (!duplicate.getGuid().equals(mainPatient.getGuid())) {
                OldClientGuid additionalGuid = OldClientGuid.builder()
                        .guid(duplicate.getGuid())
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

        return priorityPatient.orElseGet(() -> patients.stream()
                .max(Comparator.comparing(client -> dateParser.parseOldSystemTimestamp(client.getCreatedDateTime())))
                .orElse(patients.get(0)));

        // Если нет приоритетного статуса, выбираем с последней датой создания
    }

    private void validateOldClient(OldClientDto oldClient) {
        if (oldClient == null) {
            throw new InvalidDataException("Old client data is null");
        }

        if (StringUtils.isBlank(oldClient.getGuid().toString())) {
            throw new InvalidDataException("Client GUID is blank");
        }

        try {
            UUID.fromString(oldClient.getGuid().toString());
        } catch (IllegalArgumentException e) {
            throw new InvalidDataException("Invalid GUID format: " + oldClient.getGuid());
        }
    }
}