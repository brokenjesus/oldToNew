package by.lupach.oldtonew.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "old_client_guid")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OldClientGuid {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "guid", nullable = false, unique = true)
    private UUID guid;

    @Column(name = "patient_profile_id", insertable = false, updatable = false)
    private Long patientProfileId;
}