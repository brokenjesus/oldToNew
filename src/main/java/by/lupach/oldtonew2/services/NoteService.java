package by.lupach.oldtonew2.services;

import by.lupach.oldtonew2.dtos.OldNoteDto;
import by.lupach.oldtonew2.entities.CompanyUser;
import by.lupach.oldtonew2.entities.PatientNote;
import by.lupach.oldtonew2.entities.PatientProfile;
import by.lupach.oldtonew2.exceptions.InvalidDataException;
import by.lupach.oldtonew2.exceptions.NoteProcessingException;
import by.lupach.oldtonew2.repositories.CompanyUserRepository;
import by.lupach.oldtonew2.repositories.PatientNoteRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class NoteService {

    private final PatientNoteRepository noteRepo;
    private final CompanyUserRepository userRepo;

    public ImportResult upsertNote(PatientProfile patient, OldNoteDto src) {
        validateNoteData(patient, src);

        try {
            LocalDateTime created = parseNoteTimestamp(src);
            LocalDateTime modified = Optional.ofNullable(by.lupach.oldtonewimport.utils.DateParser.parseOldSystemTimestamp(src.getModifiedDateTime()))
                    .orElse(created);

            CompanyUser author = findOrCreateUser(src.getLoggedUser());

            return noteRepo.findByOldGuid(src.getGuid())
                    .map(existing -> updateExistingNote(existing, src, modified, author))
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

        if (StringUtils.isBlank(note.getGuid())) {
            throw new InvalidDataException("Note GUID cannot be blank");
        }
    }

    private LocalDateTime parseNoteTimestamp(OldNoteDto note) {
        LocalDateTime created = by.lupach.oldtonewimport.utils.DateParser.parseOldSystemTimestamp(note.getCreatedDateTime());
        if (created == null) {
            created = by.lupach.oldtonewimport.utils.DateParser.parseOldSystemTimestamp(note.getDatetime());
        }

        if (created == null) {
            throw new InvalidDataException("Cannot parse note timestamp for note: " + note.getGuid());
        }

        return created;
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
                .oldGuid(src.getGuid())
                .note(src.getComments())
                .patient(patient)
                .createdDateTime(created)
                .lastModifiedDateTime(modified)
                .createdByUser(author)
                .lastModifiedByUser(author)
                .build();
        noteRepo.save(note);
        return ImportResult.CREATED;
    }

    private CompanyUser findOrCreateUser(String login) {
        if (StringUtils.isBlank(login)) login = "unknown";
        final String normalized = login.trim();
        return userRepo.findByLogin(normalized)
                .orElseGet(() -> userRepo.save(CompanyUser.builder().login(normalized).build()));
    }

    public enum ImportResult { CREATED, UPDATED, SKIPPED }
}