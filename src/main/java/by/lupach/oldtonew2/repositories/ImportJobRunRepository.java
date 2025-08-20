package by.lupach.oldtonew2.repositories;


import by.lupach.oldtonew2.entities.ImportJobRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImportJobRunRepository extends JpaRepository<ImportJobRun, Long> { }