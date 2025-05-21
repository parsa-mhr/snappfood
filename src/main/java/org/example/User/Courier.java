package org.example.User;

import jakarta.persistence.*;

@Entity
@Table(name = "Couriers")
public class Courier extends User {
    @Id
    private Long id;
    @Embedded
    private BankInfo bankInfo;

    public Courier(String fullName, String email, String password, String phonenumber,
            String address, BankInfo bankInfo) {
        super(fullName, email, password, phonenumber, address);
        this.setRole(UserRole.courier);
        this.bankInfo = bankInfo;
    }

    public Courier() {
        super();
        this.setRole(UserRole.courier);
    }

    public BankInfo getBankInfo() {
        return bankInfo;
    }

    public void setBankInfo(BankInfo bankInfo) {
        this.bankInfo = bankInfo;
    }
}
