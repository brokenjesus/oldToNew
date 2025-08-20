package by.lupach.oldtonew2.repositories;


import by.lupach.oldtonew2.entities.CompanyUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyUserRepository extends JpaRepository<CompanyUser, Long> {
    Optional<CompanyUser> findByLogin(String login);
}