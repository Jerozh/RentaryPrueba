package BackEnd.Rentary.AutomaticTask;

import BackEnd.Rentary.BCRA.Service.BcraApiService;
import BackEnd.Rentary.Contracts.Entity.Contract;
import BackEnd.Rentary.Contracts.Enums.AdjustmentType;
import BackEnd.Rentary.Contracts.Respository.IContractRepository;
import BackEnd.Rentary.Payments.Entities.Payment;
import BackEnd.Rentary.Payments.Enums.Currency;
import BackEnd.Rentary.Payments.Enums.PaymentMethod;
import BackEnd.Rentary.Payments.Enums.PaymentStatus;
import BackEnd.Rentary.Payments.Enums.ServiceType;
import BackEnd.Rentary.Payments.Factory.PaymentFactory;
import BackEnd.Rentary.Payments.Repository.PaymentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static BackEnd.Rentary.AutomaticTask.Service.Calcs.calculateAdjustedRent;
import static BackEnd.Rentary.AutomaticTask.Service.RentAdjustmentUtils.shouldAdjustRent;

@Service
@RequiredArgsConstructor
public class RentAdjustmentScheduler {

    private final IContractRepository contractRepository;
    private final BcraApiService bcraApiService;
    private final PaymentRepository paymentRepository;

    @Transactional
    @Scheduled(cron = "0 0 3 1,3,5,7,9,10 * *", zone = "UTC")
    public void updateCurrentRentForAllContracts() {
        List<Contract> activeContracts = contractRepository.findByActiveTrue();
        Double currentIcl = bcraApiService.getCurrentIclValueBlocking();
        LocalDate now = LocalDate.now();

        List<Payment> paymentsToSave = new ArrayList<>();

        for (Contract contract : activeContracts) {
            if (contract.getAdjustmentType() == AdjustmentType.ICL && currentIcl != null && shouldAdjustRent(contract)) {
                double newRent = calculateAdjustedRent(contract, currentIcl);
                contract.setCurrentRent(newRent);
                contract.setLastAdjustmentDate(now);

                int dayOfMonth = contract.getDeadline();
                LocalDate dueDate;
                if (now.getDayOfMonth() >= dayOfMonth) {
                    LocalDate nextMonth = now.plusMonths(1);
                    int safeDay = Math.min(dayOfMonth, nextMonth.lengthOfMonth());
                    dueDate = LocalDate.of(nextMonth.getYear(), nextMonth.getMonth(), safeDay);
                } else {
                    int safeDay = Math.min(dayOfMonth, now.lengthOfMonth());
                    dueDate = LocalDate.of(now.getYear(), now.getMonth(), safeDay);
                }

                Payment payment = PaymentFactory.createPaymentEntity(
                        contract,
                        BigDecimal.valueOf(newRent),
                        dueDate,
                        ServiceType.ALQUILER,
                        PaymentMethod.TRANSFERENCIA,
                        Currency.PESOS,
                        "Pago mensual automático",
                        contract.getCreatedBy()
                );
                payment.setPaymentDate(payment.getDueDate());
                payment.setStatus(PaymentStatus.PENDIENTE);
                paymentsToSave.add(payment);
            }
        }

        contractRepository.saveAll(activeContracts);
        paymentRepository.saveAll(paymentsToSave);
    }
}