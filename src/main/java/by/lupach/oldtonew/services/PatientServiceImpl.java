package by.lupach.oldtonew.services;

import by.lupach.oldtonew.dtos.OldClientDto;
import by.lupach.oldtonew.entities.OldClientGuid;
import by.lupach.oldtonew.entities.PatientProfile;
import by.lupach.oldtonew.exceptions.InvalidDataException;
import by.lupach.oldtonew.exceptions.PatientCreationException;
import by.lupach.oldtonew.models.ImportStatistics;
import by.lupach.oldtonew.repositories.PatientProfileRepository;
import by.lupach.oldtonew.utils.DateParser;
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

        // Step 1: Find the events GUID
        List<OldClientGuid> existingOldClientGuids = oldClientGuidService.getAllClientsGuids();


        // Step 2: Search for duplicates (if full name and date of birth match)
        // from the technical task it follows that 1 patient corresponds to 1 or more clients in the old system,
        // therefore, there are duplicate users, but with different GUIDs
        // since we do not store the passport number or any other field by which we can clearly determine who the duplicate is
        // Duplicates are found by full name and date of birth
        Map<String, List<OldClientDto>> oldGroupedPatients = groupPatientsByKey(oldClients);

        // Step 3: Filter clients, find new patients and those who need to be updated
        Map<String, List<OldClientDto>> newGroupedPatients = oldGroupedPatients.entrySet().stream()
                .filter(entry -> entry.getValue().stream()
                        .noneMatch(client ->
                                existingOldClientGuids.stream().map(OldClientGuid::getGuid)
                                        .toList()
                                        .contains(client.getGuid()))
                )
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Set<String> keysToRemove = oldGroupedPatients.keySet().stream()
                .filter(newGroupedPatients::containsKey)
                .collect(Collectors.toSet());

        oldGroupedPatients.keySet().removeAll(keysToRemove);
        statistics.incrementSkippedPatients(keysToRemove.size());

        //The old system might have created a duplicate after the patient was added to the new one, and our patient
        // does not reference those GUIDs, so the notes might be lost, so we need to add the missing GUID to the patient
        Map<String, List<OldClientDto>> oldGroupedPatientsToUpdate = oldGroupedPatients.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .filter(entry -> entry.getValue().stream()
                        .anyMatch(client -> !existingOldClientGuids.stream().map(OldClientGuid::getGuid)
                                .toList()
                                .contains(client.getGuid()))
                )
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // Step 4: Process each group

        List<PatientProfile> resultPatients = new ArrayList<>();
        for (List<OldClientDto> group : oldGroupedPatientsToUpdate.values()) {
            {
                resultPatients.add(mergeAndUpdate(group));
            }
        }
        statistics.incrementUpdatedPatients(resultPatients.size());

        for (List<OldClientDto> group : newGroupedPatients.values()) {
            if (group.size() == 1) {
                resultPatients.add(save(group.get(0)));
            } else {
                resultPatients.add(mergeAndSave(group));
            }
        }
        statistics.incrementNewPatients(resultPatients.size()-statistics.getUpdatedPatients());

        return resultPatients;
    }

    private PatientProfile mergeAndUpdate(List<OldClientDto> duplicates) {
        Long idInNewSystem = duplicates.stream()
                .map(OldClientDto::getGuid)
                .map(oldClientGuidService::getByGuid)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(oldClientGuid -> oldClientGuid.getPatientProfile().getId())
                .findFirst()
                .orElse(null);

        if (idInNewSystem == null) {
            throw new InvalidDataException("No patient was found found");
        }

        PatientProfile patientProfile = getByIdInNewSystem(idInNewSystem);
        List<OldClientGuid> guids = oldClientGuidService.getAllClientGuids(patientProfile.getId());


        OldClientDto mainPatient = selectMainPatient(duplicates);
        patientProfile.setStatusId(mainPatient.getStatus());

        for (OldClientDto duplicate : duplicates) {
            if (!guids.stream().map(OldClientGuid::getGuid).toList().contains(duplicate.getGuid())) {
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
        OldClientDto mainPatient = selectMainPatient(duplicates);

        PatientProfile patient = save(mainPatient);

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
        Optional<OldClientDto> priorityPatient = patients.stream()
                .filter(client -> client.getStatus() == 210 ||
                        client.getStatus() == 220 ||
                        client.getStatus() == 230)
                .findFirst();

        return priorityPatient.orElseGet(() -> patients.stream()
                .max(Comparator.comparing(client -> dateParser.parseOldSystemTimestamp(client.getCreatedDateTime())))
                .orElse(patients.get(0)));

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