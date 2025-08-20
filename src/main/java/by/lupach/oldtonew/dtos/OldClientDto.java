package by.lupach.oldtonew.dtos;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class OldClientDto {
    private String agency;
    private UUID guid;
    private String firstName;
    private String lastName;
    private Short status;
    private String dob;
    private String createdDateTime;
}