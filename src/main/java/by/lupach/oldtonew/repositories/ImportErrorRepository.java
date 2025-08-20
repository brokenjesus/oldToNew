package by.lupach.oldtonew.repositories;

import by.lupach.oldtonew.entities.ImportError;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImportErrorRepository extends JpaRepository<ImportError, Long> { }
