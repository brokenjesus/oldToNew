package by.lupach.oldtonew.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "company_user")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CompanyUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String login;
}