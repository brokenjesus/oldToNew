package by.lupach.oldtonew2.services;

import by.lupach.oldtonew2.dtos.OldClientDto;
import by.lupach.oldtonew2.entities.PatientProfile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface PatientService {
    Map<UUID, PatientProfile> buildGuidToPatientMap();

    List<PatientProfile> savePatients(List<OldClientDto> oldClients);
}
