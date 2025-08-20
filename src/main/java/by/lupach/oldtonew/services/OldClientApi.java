package by.lupach.oldtonew.services;

import by.lupach.oldtonew.dtos.OldClientDto;
import by.lupach.oldtonew.dtos.OldNoteDto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface OldClientApi {
    List<OldClientDto> getAllClients();

    List<OldNoteDto> getClientNotes(String agency, UUID clientGuid, LocalDate from, LocalDate to);
}
