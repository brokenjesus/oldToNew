package by.lupach.oldtonew.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;
@Entity
@Table(name = "old_note_guid")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OldNoteGuid {
    @Id
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_note_id")
    private PatientNote patientNote;

    @Column(name = "guid", nullable = false, unique = true)
    private UUID guid;
}