package by.lupach.oldtonew.services;

import by.lupach.oldtonew.dtos.OldNoteDto;
import by.lupach.oldtonew.entities.PatientProfile;

public interface NoteService {
    NoteServiceImpl.ImportResult upsertNote(PatientProfile patient, OldNoteDto src);
}
