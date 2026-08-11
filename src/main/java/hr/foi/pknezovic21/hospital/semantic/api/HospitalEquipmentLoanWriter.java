package hr.foi.pknezovic21.hospital.semantic.api;

import hr.foi.pknezovic21.hospital.domain.EquipmentLoanForm;

public interface HospitalEquipmentLoanWriter {

    void addEquipmentLoan(String id, String loanNumber, String loanedAt, EquipmentLoanForm form);
}
