package by.lupach.oldtonew.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "import_error")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ImportError {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_run_id", nullable = false)
    private ImportJobRun jobRun;

    private LocalDateTime errorTime;
    private UUID noteGuid;
    private Long patientId;
    private UUID clientGuid;

    @Column(columnDefinition = "text")
    private String message;

    @Column(columnDefinition = "text")
    private String stacktrace;
}
