package by.lupach.oldtonew.services;

import by.lupach.oldtonew.dtos.OldNoteDto;
import by.lupach.oldtonew.entities.CompanyUser;
import by.lupach.oldtonew.entities.PatientNote;
import by.lupach.oldtonew.entities.PatientProfile;
import by.lupach.oldtonew.exceptions.InvalidDataException;
import by.lupach.oldtonew.exceptions.NoteProcessingException;
import by.lupach.oldtonew.repositories.CompanyUserRepository;
import by.lupach.oldtonew.repositories.PatientNoteRepository;
import by.lupach.oldtonew.utils.DateParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoteServiceImplTest {

    @Mock
    private PatientNoteRepository noteRepo;

    @Mock
    private CompanyUserRepository userRepo;

    @Mock
    private DateParser dateParser;

    @InjectMocks
    private NoteServiceImpl noteService;

    private PatientProfile testPatient;
    private OldNoteDto testNoteDto;
    private CompanyUser testUser;
    private LocalDateTime testCreatedDate;
    private LocalDateTime testModifiedDate;

    @BeforeEach
    void setUp() {
        testPatient = PatientProfile.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .build();

        testNoteDto = OldNoteDto.builder()
                .guid(UUID.randomUUID())
                .comments("Test note content")
                .createdDateTime("2023-01-01T10:00:00")
                .modifiedDateTime("2023-01-02T10:00:00")
                .loggedUser("testuser")
                .build();

        testUser = CompanyUser.builder()
                .id(1L)
                .login("testuser")
                .build();

        testCreatedDate = LocalDateTime.of(2023, 1, 1, 10, 0, 0);
        testModifiedDate = LocalDateTime.of(2023, 1, 2, 10, 0, 0);
    }

    @Test
    void upsertNote_WithNullPatient_ShouldThrowInvalidDataException() {
        assertThrows(InvalidDataException.class, () ->
                noteService.upsertNote(null, testNoteDto));
    }

    @Test
    void upsertNote_WithNullNote_ShouldThrowInvalidDataException() {
        assertThrows(InvalidDataException.class, () ->
                noteService.upsertNote(testPatient, null));
    }

    @Test
    void upsertNote_NewNote_ShouldCreateNote() {
        when(dateParser.parseOldSystemTimestamp(eq("2023-01-01T10:00:00")))
                .thenReturn(testCreatedDate);
        when(dateParser.parseOldSystemTimestamp(eq("2023-01-02T10:00:00")))
                .thenReturn(testModifiedDate);
        when(userRepo.findByLogin("testuser")).thenReturn(Optional.of(testUser));
        when(noteRepo.findByOldGuid_Guid(testNoteDto.getGuid())).thenReturn(Optional.empty());
        when(noteRepo.save(any(PatientNote.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NoteServiceImpl.ImportResult result = noteService.upsertNote(testPatient, testNoteDto);

        assertEquals(NoteServiceImpl.ImportResult.CREATED, result);
        verify(noteRepo, times(2)).save(any(PatientNote.class));
        verify(userRepo).findByLogin("testuser");
    }

    @Test
    void upsertNote_ExistingNote_NewerModifiedDate_ShouldUpdateNote() {
        when(dateParser.parseOldSystemTimestamp(eq("2023-01-01T10:00:00")))
                .thenReturn(testCreatedDate);
        when(dateParser.parseOldSystemTimestamp(eq("2023-01-02T10:00:00")))
                .thenReturn(testModifiedDate);

        PatientNote existingNote = PatientNote.builder()
                .id(1L)
                .note("Old content")
                .lastModifiedDateTime(LocalDateTime.of(2023, 1, 1, 9, 0, 0))
                .build();

        when(noteRepo.findByOldGuid_Guid(testNoteDto.getGuid())).thenReturn(Optional.of(existingNote));
        when(noteRepo.save(any(PatientNote.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NoteServiceImpl.ImportResult result = noteService.upsertNote(testPatient, testNoteDto);

        assertEquals(NoteServiceImpl.ImportResult.UPDATED, result);
        verify(noteRepo).save(existingNote);
        assertEquals("Test note content", existingNote.getNote());
        assertEquals(testModifiedDate, existingNote.getLastModifiedDateTime());
    }

    @Test
    void upsertNote_ExistingNote_OlderModifiedDate_ShouldSkipUpdate() {
        when(dateParser.parseOldSystemTimestamp(eq("2023-01-01T10:00:00")))
                .thenReturn(testCreatedDate);
        when(dateParser.parseOldSystemTimestamp(eq("2023-01-02T10:00:00")))
                .thenReturn(testModifiedDate);

        PatientNote existingNote = PatientNote.builder()
                .id(1L)
                .note("Old content")
                .lastModifiedDateTime(LocalDateTime.of(2023, 1, 3, 10, 0, 0))
                .build();

        when(noteRepo.findByOldGuid_Guid(testNoteDto.getGuid())).thenReturn(Optional.of(existingNote));

        NoteServiceImpl.ImportResult result = noteService.upsertNote(testPatient, testNoteDto);

        assertEquals(NoteServiceImpl.ImportResult.SKIPPED, result);
        verify(noteRepo, never()).save(any(PatientNote.class));
        assertEquals("Old content", existingNote.getNote());
    }

    @Test
    void upsertNote_WithUnknownUser_ShouldCreateNewUser() {
        OldNoteDto noteWithUnknownUser = OldNoteDto.builder()
                .guid(UUID.randomUUID())
                .comments("Test")
                .createdDateTime("2023-01-01T10:00:00")
                .modifiedDateTime("2023-01-02T10:00:00")
                .loggedUser("unknownuser")
                .build();

        CompanyUser newUser = CompanyUser.builder()
                .id(2L)
                .login("unknownuser")
                .build();

        when(dateParser.parseOldSystemTimestamp(eq("2023-01-01T10:00:00"))).thenReturn(testCreatedDate);
        when(dateParser.parseOldSystemTimestamp(eq("2023-01-02T10:00:00"))).thenReturn(testModifiedDate);
        when(userRepo.findByLogin("unknownuser")).thenReturn(Optional.empty());
        when(userRepo.save(any(CompanyUser.class))).thenReturn(newUser);
        when(noteRepo.findByOldGuid_Guid(noteWithUnknownUser.getGuid())).thenReturn(Optional.empty());
        when(noteRepo.save(any(PatientNote.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NoteServiceImpl.ImportResult result = noteService.upsertNote(testPatient, noteWithUnknownUser);

        assertEquals(NoteServiceImpl.ImportResult.CREATED, result);
        verify(userRepo).save(any(CompanyUser.class));
    }

    @Test
    void upsertNote_WithBlankUser_ShouldUseDefaultUser() {
        OldNoteDto noteWithBlankUser = OldNoteDto.builder()
                .guid(UUID.randomUUID())
                .comments("Test")
                .createdDateTime("2023-01-01T10:00:00")
                .modifiedDateTime("2023-01-02T10:00:00")
                .loggedUser("")
                .build();

        CompanyUser defaultUser = CompanyUser.builder()
                .id(3L)
                .login("unknown")
                .build();

        when(dateParser.parseOldSystemTimestamp(eq("2023-01-01T10:00:00"))).thenReturn(testCreatedDate);
        when(dateParser.parseOldSystemTimestamp(eq("2023-01-02T10:00:00"))).thenReturn(testModifiedDate);
        when(userRepo.findByLogin("unknown")).thenReturn(Optional.of(defaultUser));
        when(noteRepo.findByOldGuid_Guid(noteWithBlankUser.getGuid())).thenReturn(Optional.empty());
        when(noteRepo.save(any(PatientNote.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NoteServiceImpl.ImportResult result = noteService.upsertNote(testPatient, noteWithBlankUser);

        assertEquals(NoteServiceImpl.ImportResult.CREATED, result);
        verify(userRepo).findByLogin("unknown");
    }

    @Test
    void upsertNote_DateParsingException_ShouldThrowNoteProcessingException() {
        when(dateParser.parseOldSystemTimestamp(anyString()))
                .thenThrow(new RuntimeException("Date parsing failed"));

        assertThrows(NoteProcessingException.class, () ->
                noteService.upsertNote(testPatient, testNoteDto));
    }

    @Test
    void upsertNote_DatabaseException_ShouldThrowNoteProcessingException() {
        when(dateParser.parseOldSystemTimestamp(eq("2023-01-01T10:00:00"))).thenReturn(testCreatedDate);
        when(dateParser.parseOldSystemTimestamp(eq("2023-01-02T10:00:00"))).thenReturn(testModifiedDate);
        when(userRepo.findByLogin("testuser")).thenReturn(Optional.of(testUser));
        when(noteRepo.findByOldGuid_Guid(testNoteDto.getGuid())).thenReturn(Optional.empty());
        when(noteRepo.save(any(PatientNote.class))).thenThrow(new RuntimeException("DB error"));

        assertThrows(NoteProcessingException.class, () ->
                noteService.upsertNote(testPatient, testNoteDto));
    }

    @Test
    void findOrCreateUser_ExistingUser_ShouldReturnUser() throws Exception {
        when(userRepo.findByLogin("existinguser")).thenReturn(Optional.of(testUser));

        Method method = NoteServiceImpl.class.getDeclaredMethod("findOrCreateUser", String.class);
        method.setAccessible(true);

        CompanyUser result = (CompanyUser) method.invoke(noteService, "existinguser");

        assertEquals(testUser, result);
        verify(userRepo, never()).save(any());
    }

    @Test
    void findOrCreateUser_NewUser_ShouldCreateAndReturnUser() throws Exception {
        String newLogin = "newuser";
        CompanyUser newUser = CompanyUser.builder().login(newLogin).build();

        when(userRepo.findByLogin(newLogin)).thenReturn(Optional.empty());
        when(userRepo.save(any(CompanyUser.class))).thenReturn(newUser);

        Method method = NoteServiceImpl.class.getDeclaredMethod("findOrCreateUser", String.class);
        method.setAccessible(true);

        CompanyUser result = (CompanyUser) method.invoke(noteService, newLogin);

        assertNotNull(result);
        assertEquals(newLogin, result.getLogin());
        verify(userRepo).save(any(CompanyUser.class));
    }

    @Test
    void findOrCreateUser_BlankLogin_ShouldUseDefault() throws Exception {
        CompanyUser defaultUser = CompanyUser.builder().login("unknown").build();

        when(userRepo.findByLogin("unknown")).thenReturn(Optional.of(defaultUser));

        Method method = NoteServiceImpl.class.getDeclaredMethod("findOrCreateUser", String.class);
        method.setAccessible(true);

        CompanyUser result = (CompanyUser) method.invoke(noteService, "");

        assertEquals("unknown", result.getLogin());
    }

    @Test
    void findOrCreateUser_NullLogin_ShouldUseDefault() throws Exception {
        CompanyUser defaultUser = CompanyUser.builder().login("unknown").build();

        when(userRepo.findByLogin("unknown")).thenReturn(Optional.of(defaultUser));

        Method method = NoteServiceImpl.class.getDeclaredMethod("findOrCreateUser", String.class);
        method.setAccessible(true);

        CompanyUser result = (CompanyUser) method.invoke(noteService, new Object[]{null});

        assertEquals("unknown", result.getLogin());
    }
}