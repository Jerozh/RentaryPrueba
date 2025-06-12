package BackEnd.Rentary.AutomaticTask;

import BackEnd.Rentary.Contracts.Entity.Contract;
import BackEnd.Rentary.Contracts.Respository.IContractRepository;
import BackEnd.Rentary.Properties.Entities.Property;
import BackEnd.Rentary.Properties.Enums.PropertyStatus;
import BackEnd.Rentary.Properties.Repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContractStatusScheduler {

    private final IContractRepository contractRepository;
    private final PropertyRepository propertyRepository;

    @Transactional
    @Scheduled(cron = "0 0 6 * * ?", zone = "UTC")
    public void updateContractStatus() {
        List<Contract> activeContracts = contractRepository.findByActiveTrue();
        LocalDate today = LocalDate.now();
        List<Contract> contractsToSave = new ArrayList<>();
        List<Property> propertiesToSave = new ArrayList<>();

        for (Contract contract : activeContracts) {
            if (!contract.getEndDate().isAfter(today)) {
                contract.setActive(false);
                contractsToSave.add(contract);

                Property property = contract.getProperty();
                property.setStatus(PropertyStatus.DISPONIBLE);
                propertiesToSave.add(property);
            }
        }
        if(!contractsToSave.isEmpty()){
            contractRepository.saveAll(contractsToSave);
        }
        if (!propertiesToSave.isEmpty()) {
        propertyRepository.saveAll(propertiesToSave);
        }
    }
}

