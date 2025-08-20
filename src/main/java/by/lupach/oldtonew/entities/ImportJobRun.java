package by.lupach.oldtonew.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "import_job_run")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportJobRun {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String status;
    private int newCount;
    private int updatedCount;
    private int skippedCount;
    private int errorCount;
}