package org.example.Restaurant;

import jakarta.persistence.*;
import org.example.User.Seller;
import org.hibernate.Session;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Resturants")

public abstract class Restaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(unique = true, nullable = false)
    private String phone;

    @ElementCollection
    private List<String> workingHours = new ArrayList<>();

    @ElementCollection
    private List<String> categories = new ArrayList<>();

    @Lob
    @Column(name = "logo", columnDefinition = "LONGBLOB")
    private byte[] logo;

    @ManyToOne
    private Seller seller;

    @Transient
    private String profileImageBase64;

    @Transient
    private Menu menu;

    private double tax_fee;

    private double additional_fee;

    private Double rating;
    private Double deliveryFee;

//    @OneToMany(mappedBy = "restaurant", cascade = CascadeType.ALL, orphanRemoval = true)
//    private List<MenuItem> menuItems = new ArrayList<>();

    public Restaurant() {
    }

    public Restaurant(String name, String address, String phone , Seller seller ) {
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.seller = seller;
    }

    public Long getId() {
        return id;
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

    public List<String> getWorkingHours() {
        return workingHours;
    }

    public void setWorkingHours(List<String> workingHours) {
        this.workingHours = workingHours;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    public byte[] getLogo() {
        return logo;
    }

    public void setLogo(byte[] logo) {
        this.logo = logo;
    }

    public String getProfileImageBase64() {
        return profileImageBase64;
    }

    public void setProfileImageBase64(String profileImageBase64) {
        this.profileImageBase64 = profileImageBase64;
    }
//
//    public List<MenuItem> getMenuItems() {
//        return menuItems;
//    }
//
//    public void setMenuItems(List<MenuItem> menuItems) {
//        this.menuItems = menuItems;
//    }
public void loadMenu(Session session) {
    this.menu = new Menu(this.id, session);
}

    public Menu getMenu() {
        return menu;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Seller getSeller() {
        return seller;
    }

    public void setSeller(Seller seller) {
        this.seller = seller;
    }

    public void setMenu(Menu menu) {
        this.menu = menu;
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

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public Double getDeliveryFee() {
        return deliveryFee;
    }

    public void setDeliveryFee(Double deliveryFee) {
        this.deliveryFee = deliveryFee;
    }
}
