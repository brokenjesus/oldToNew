package by.lupach.oldtonew2.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "old_note_guid")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OldNoteGuid {
}
