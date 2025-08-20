package by.lupach.oldtonew2.services;

import by.lupach.oldtonew2.dtos.OldClientDto;
import by.lupach.oldtonew2.dtos.OldNoteDto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface OldClientApi {
    List<OldClientDto> getAllClients();

    List<OldNoteDto> getClientNotes(String agency, UUID clientGuid, LocalDate from, LocalDate to);
}
