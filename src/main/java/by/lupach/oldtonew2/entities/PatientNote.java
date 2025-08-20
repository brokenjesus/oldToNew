package by.lupach.oldtonew2.entities;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "patient_note")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PatientNote {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_date_time", nullable = false)
    private LocalDateTime createdDateTime;

    @Column(name = "last_modified_date_time", nullable = false)
    private LocalDateTime lastModifiedDateTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private CompanyUser createdByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_modified_by_user_id")
    private CompanyUser lastModifiedByUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private PatientProfile patient;

    @Column(name = "note", length = 4000)
    private String note;

    @OneToOne(mappedBy = "patientNote", cascade = CascadeType.ALL, orphanRemoval = true)
    private OldNoteGuid oldGuid;
}
