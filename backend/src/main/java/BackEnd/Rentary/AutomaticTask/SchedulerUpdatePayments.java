package BackEnd.Rentary.AutomaticTask;

import BackEnd.Rentary.Payments.Entities.Payment;
import BackEnd.Rentary.Payments.Enums.PaymentStatus;
import BackEnd.Rentary.Payments.Repository.PaymentRepository;
import BackEnd.Rentary.Payments.Utils.PaymentCalculationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SchedulerUpdatePayments {

    private final PaymentRepository paymentRepository;

    @Scheduled(cron = "0 0 4 * * *", zone = "UTC")
    @Transactional
    public void updateOverduePayments() {
        LocalDate today = LocalDate.now();
        List<Payment> pendingPayments = paymentRepository.findByStatus(PaymentStatus.PENDIENTE);

        List<Payment> overduePayments = pendingPayments.stream()
                .filter(payment -> PaymentCalculationUtil.isPaymentOverdue(payment, today))
                .peek(payment -> payment.setStatus(PaymentStatus.VENCIDO))
                .collect(Collectors.toList());

        if (!overduePayments.isEmpty()) {
            paymentRepository.saveAll(overduePayments);
        }
    }
}
