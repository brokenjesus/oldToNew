package by.lupach.oldtonew2.services;

import by.lupach.oldtonew2.dtos.OldNoteDto;
import by.lupach.oldtonew2.entities.PatientProfile;

public interface NoteService {
    NoteServiceImpl.ImportResult upsertNote(PatientProfile patient, OldNoteDto src);
}
