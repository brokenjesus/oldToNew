package by.lupach.oldtonew.services;

import by.lupach.oldtonew.dtos.OldNoteDto;
import by.lupach.oldtonew.entities.CompanyUser;
import by.lupach.oldtonew.entities.OldNoteGuid;
import by.lupach.oldtonew.entities.PatientNote;
import by.lupach.oldtonew.entities.PatientProfile;
import by.lupach.oldtonew.exceptions.InvalidDataException;
import by.lupach.oldtonew.exceptions.NoteProcessingException;
import by.lupach.oldtonew.repositories.CompanyUserRepository;
import by.lupach.oldtonew.repositories.PatientNoteRepository;
import by.lupach.oldtonew.utils.DateParser;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NoteServiceImpl implements NoteService {

    private final PatientNoteRepository noteRepo;
    private final CompanyUserRepository userRepo;
    private final DateParser dateParser;

    @Override
    public ImportResult upsertNote(PatientProfile patient, OldNoteDto src) {
        validateNoteData(patient, src);

        try {
            LocalDateTime created = dateParser.parseOldSystemTimestamp(src.getCreatedDateTime());
            LocalDateTime modified = dateParser.parseOldSystemTimestamp(src.getModifiedDateTime());

            CompanyUser author = findOrCreateUser(src.getLoggedUser());

            return noteRepo.findByOldGuid_Guid(src.getGuid())
                    .map(note -> updateExistingNote(note, src, modified, author))
                    .orElseGet(() -> createNewNote(patient, src, created, modified, author));
        } catch (Exception e) {
            String errorMsg = String.format("Failed to upsert note for patient %s: %s",
                    patient.getId(), e.getMessage());
            throw new NoteProcessingException(errorMsg, e);
        }
    }


    private void validateNoteData(PatientProfile patient, OldNoteDto note) {
        if (patient == null) {
            throw new InvalidDataException("Patient cannot be null");
        }

        if (note == null) {
            throw new InvalidDataException("Note data cannot be null");
        }

        if (StringUtils.isBlank(note.getGuid().toString())) {
            throw new InvalidDataException("Note GUID cannot be blank");
        }
    }

    private ImportResult updateExistingNote(PatientNote existing, OldNoteDto src, LocalDateTime modified, CompanyUser author) {
        if (modified.isAfter(existing.getLastModifiedDateTime())) {
            existing.setNote(src.getComments());
            existing.setLastModifiedDateTime(modified);
            existing.setLastModifiedByUser(author);
            noteRepo.save(existing);
            return ImportResult.UPDATED;
        } else {
            return ImportResult.SKIPPED;
        }
    }

    private ImportResult createNewNote(PatientProfile patient, OldNoteDto src,
                                       LocalDateTime created, LocalDateTime modified, CompanyUser author) {

        PatientNote note = PatientNote.builder()
                .note(src.getComments())
                .patient(patient)
                .createdDateTime(created)
                .lastModifiedDateTime(modified)
                .createdByUser(author)
                .lastModifiedByUser(author)
                .build();

        note = noteRepo.save(note);

        OldNoteGuid oldGuid = OldNoteGuid.builder()
                .guid(src.getGuid())
                .patientNote(note)
                .build();

        note.setOldGuid(oldGuid);

        noteRepo.save(note);

        return ImportResult.CREATED;
    }

    private CompanyUser findOrCreateUser(String login) {
        if (StringUtils.isBlank(login)) login = "unknown";
        final String normalized = login.trim();
        return userRepo.findByLogin(normalized)
                .orElseGet(() -> userRepo.save(CompanyUser.builder().login(normalized).build()));
    }

    public enum ImportResult {CREATED, UPDATED, SKIPPED}
}