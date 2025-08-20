package by.lupach.oldtonew2.entities;

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
public class OldClientGuid { //вынес в отдельную таблицу, т.к. хранить в строке неатомарные данные нарушает 1NF sql
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "guid", nullable = false, unique = true)
    private UUID guid;

    @Column(name = "patient_profile_id", insertable = false, updatable = false)
    private Long patientProfileId;
}