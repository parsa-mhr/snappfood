package org.example.User;

import jakarta.persistence.*;

@Entity
@PrimaryKeyJoinColumn(name = "id") // ✅ این خط مهمه
@Table(name = "Couriers")
public class Courier extends User {

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "bankName", column = @Column(name = "bank_name")),
            @AttributeOverride(name = "accountNumber", column = @Column(name = "account_number"))
    })
    private BankInfo bankInformation;

    public Courier() {
        super();
    }

    public Courier(String name, String email, String password, String phone, String address, BankInfo bankInformation) {
        super(name, email, password, phone, address);
        this.bankInformation = bankInformation;
    }

    public BankInfo getBankInformation() {
        return bankInformation;
    }

    public void setBankInformation(BankInfo bankInformation) {
        this.bankInformation = bankInformation;
    }
}
    