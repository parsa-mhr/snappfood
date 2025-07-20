package org.example.Models;

public class VendorsDto {
    private long id;
    private String name;
    private String address;
    private String phone;
    private String logoBase64;
    private double tax_fee;
    private double additional_fee;

    public VendorsDto(long id, String name, String address, String phone, String logoBase64, double tax_fee, double additional_fee) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.logoBase64 = logoBase64;
        this.tax_fee = tax_fee;
        this.additional_fee = additional_fee;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getLogoBase64() {
        return logoBase64;
    }

    public void setLogoBase64(String logoBase64) {
        this.logoBase64 = logoBase64;
    }

    public double getTax_fee() {
        return tax_fee;
    }

    public void setTax_fee(double tax_fee) {
        this.tax_fee = tax_fee;
    }

    public double getAdditional_fee() {
        return additional_fee;
    }

    public void setAdditional_fee(double additional_fee) {
        this.additional_fee = additional_fee;
    }
}
