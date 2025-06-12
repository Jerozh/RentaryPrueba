package BackEnd.Rentary.AutomaticTask;

import BackEnd.Rentary.Contracts.Entity.Contract;
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

import static BackEnd.Rentary.AutomaticTask.Service.RentAdjustmentUtils.shouldAdjustRent;

@Service
@RequiredArgsConstructor
public class CreatePaymentScheduler {
    private final IContractRepository contractRepository;
    private final PaymentRepository paymentRepository;

    @Scheduled(cron = "0 0 5 1 * *", zone = "UTC")
    @Transactional
    public void updatePayments() {
        List<Contract> activeContracts = contractRepository.findByActiveTrue();
        LocalDate now = LocalDate.now();

        List<Payment> paymentsToSave = new ArrayList<>();

        for (Contract contract : activeContracts) {
            if (shouldAdjustRent(contract)) {
                continue;
            }

            int dayOfMonth = contract.getDeadline();
            int safeDay = Math.min(dayOfMonth, now.lengthOfMonth());
            LocalDate dueDate = LocalDate.of(now.getYear(), now.getMonth(), safeDay);

            if (paymentRepository.existsByContractAndDueDate(contract, dueDate)) {
                continue;
            }

            Payment payment = PaymentFactory.createPaymentEntity(
                    contract,
                    BigDecimal.valueOf(contract.getCurrentRent()),
                    dueDate,
                    ServiceType.ALQUILER,
                    PaymentMethod.TRANSFERENCIA,
                    Currency.PESOS,
                    "Pago mensual automático",
                    contract.getCreatedBy()
            );

            payment.setPaymentDate(dueDate);
            payment.setStatus(PaymentStatus.PENDIENTE);
            paymentsToSave.add(payment);
        }

        if (!paymentsToSave.isEmpty()) {
            paymentRepository.saveAll(paymentsToSave);
        }
    }
}
