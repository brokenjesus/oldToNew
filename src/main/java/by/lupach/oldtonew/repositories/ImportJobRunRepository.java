package by.lupach.oldtonew.repositories;


import by.lupach.oldtonew.entities.ImportJobRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImportJobRunRepository extends JpaRepository<ImportJobRun, Long> { }