package by.lupach.oldtonew2.dtos;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class OldNoteDto {
    private String comments;
    private UUID guid;
    private String modifiedDateTime;
    private String clientGuid;
    private String datetime; // может содержать зону (например, CDT)
    private String loggedUser;
    private String createdDateTime;
}