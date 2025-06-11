package BackEnd.Rentary.AutomaticTask.Service;

import BackEnd.Rentary.Contracts.Entity.Contract;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class RentAdjustmentUtils{
    public static boolean shouldAdjustRent(Contract contract) {
        LocalDate now = LocalDate.now();
        LocalDate lastAdjustment = contract.getLastAdjustmentDate();
        int frequencyMonths = contract.getAdjustmentFrequency().getMonths();

        if (lastAdjustment == null) return true;

        long monthsElapsed = ChronoUnit.MONTHS.between(lastAdjustment, now);

        return monthsElapsed >= frequencyMonths;
    }
}

