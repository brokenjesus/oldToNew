package by.lupach.oldtonew2.models;

import lombok.Getter;
import org.springframework.stereotype.Component;

@Getter
@Component
public class ImportStatistics {
    private int newNotes;
    private int updatedNotes;
    private int skippedNotes;

    private int newPatients;
    private int updatedPatients;
    private int skippedPatients;

    private int errorCount;

    public ImportStatistics() {
        this(0, 0, 0, 0, 0, 0, 0);
    }

    public ImportStatistics(int newNotes, int updatedNotes, int skippedNotes,
                            int newPatients, int updatedPatients,  int skippedPatients,
                            int errorCount) {
        this.newNotes = newNotes;
        this.updatedNotes = updatedNotes;
        this.skippedNotes = skippedNotes;

        this.newPatients = newPatients;
        this.updatedPatients = updatedPatients;
        this.skippedPatients = skippedPatients;

        this.errorCount = errorCount;
    }

    public void reset(){
        newNotes = 0;
        updatedNotes = 0;
        skippedNotes = 0;

        newPatients = 0;
        updatedPatients = 0;
        skippedPatients = 0;

        errorCount = 0;
    }

    public void incrementNewNotes() {
        newNotes++;
    }

    public void incrementNewNotes(int increment) {
        newNotes +=increment;
    }

    public void incrementUpdatedNotes() {
        updatedNotes++;
    }

    public void incrementUpdatedNotes(int increment) {
        updatedNotes +=increment;
    }

    public void incrementSkippedNotes() {
        skippedNotes++;
    }

    public void incrementSkippedNotes(int increment) {
        skippedNotes +=increment;
    }

    public void incrementError() {
        errorCount++;
    }

    public void incrementNewPatients() {
        newPatients++;
    }

    public void incrementNewPatients(int increment) {
        newPatients +=increment;
    }

    public void incrementUpdatedPatients() {
        updatedPatients++;
    }

    public void incrementUpdatedPatients(int increment) {
        updatedPatients +=increment;
    }

    public void incrementSkippedPatients() {
        skippedPatients++;
    }

    public void incrementSkippedPatients(int increment) {
        skippedPatients +=increment;
    }


    @Override
    public String toString() {
        return "ImportStatistics{" +
                "newPatients=" + newPatients +
                ", skippedPatients=" + skippedPatients +
                ", newNotes=" + newNotes +
                ", updatedNotes=" + updatedNotes +
                ", skippedNotes=" + skippedNotes +
                ", errorCount=" + errorCount +
                '}';
    }
}
