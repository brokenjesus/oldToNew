package by.lupach.oldtonew.services;

import by.lupach.oldtonew.dtos.OldClientDto;
import by.lupach.oldtonew.entities.PatientProfile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface PatientService {
    Map<UUID, PatientProfile> buildGuidToPatientMap();

    List<PatientProfile> savePatients(List<OldClientDto> oldClients);
}
