package org.example.Restaurant;

import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import org.example.User.Seller;
import org.hibernate.Session;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * کلاس Restaurant برای مدل‌سازی یک رستوران در سیستم سفارش غذا
 * این کلاس با schema رستوران در مشخصات OpenAPI همخوانی دارد
 */
@Entity
@Table(name = "restaurants")
public class Restaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Expose
    private Long id; // شناسه یکتا برای رستوران

    @Column(nullable = false)
    @Expose
    private String name; // نام رستوران (اجباری)

    @Column(nullable = false)
    @Expose
    private String address; // آدرس رستوران (اجباری)

    @Column(unique = true, nullable = false)
    @Expose
    private String phone; // شماره تلفن رستوران (اجباری و یکتا)

    @Column(name = "tax_fee", nullable = false)
    @Expose
    private Integer tax_fee; // هزینه مالیات رستوران (اجباری)

    @Column(name = "additional_fee", nullable = false)
    @Expose
    private Integer additional_fee; // هزینه اضافی رستوران (اجباری)

    @Column
    private String status ;

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getTax_fee() {
        return tax_fee;
    }

    public void setTax_fee(Integer tax_fee) {
        this.tax_fee = tax_fee;
    }

    public Integer getAdditional_fee() {
        return additional_fee;
    }

    public void setAdditional_fee(Integer additional_fee) {
        this.additional_fee = additional_fee;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
    }

    public void setMenu(Menu menu) {
        this.menu = menu;
    }


    @ManyToOne
    @JoinColumn(name = "seller_id", nullable = false)
    @com.google.gson.annotations.Expose(serialize = false, deserialize = false)
    private Seller seller; // فروشنده مرتبط با رستوران

    // فیلد کمکی برای JSON، فقط مقدار sellerId را برمی‌گرداند
    @Transient
    @com.google.gson.annotations.SerializedName("sellerId")
    @com.google.gson.annotations.Expose
    private Long sellerId;

    @Column(columnDefinition = "LONGTEXT")
    private String logoBase64; // لوگوی رستوران به‌صورت رشته Base64 برای پاسخ‌های API

    @Transient
    private Menu menu; // منوی رستوران که به‌صورت پویا بارگذاری می‌شود

    // مقداردهی sellerId پس از بارگذاری از دیتابیس
    @PostLoad
    public void fillSellerId() {
        this.sellerId = (seller != null) ? seller.getId() : null;
    }

    // سازنده پیش‌فرض
    public Restaurant() {
    }

    // سازنده با پارامترهای اصلی
    public Restaurant(String name, String address, String phone, Integer tax_fee, Integer additional_fee, Seller seller) {
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.tax_fee = tax_fee;
        this.additional_fee = additional_fee;
        this.seller = seller;
    }

    // getter و setterها

    public Long getId() {
        return id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPhone() {
        return phone;
    }

    public void setTaxFee(Integer tax_fee) {
        this.tax_fee = tax_fee;
    }

    public Integer getTaxFee() {
        return tax_fee;
    }

    public void setAdditionalFee(Integer additional_fee) {
        this.additional_fee = additional_fee;
    }

    public Integer getAdditionalFee() {
        return additional_fee;
    }

//    public void setWorkingHours(List<String> workingHours) {
//        this.workingHours = workingHours;
//    }
//
//    public List<String> getWorkingHours() {
//        return workingHours;
//    }

//    public void setCategories(List<String> categories) {
//        this.categories = categories;
//    }
//
//    public List<String> getCategories() {
//        return categories;
//    }


    public void setLogoBase64(String logoBase64) {
        this.logoBase64 = logoBase64;
    }

    public String getLogoBase64() {
        return logoBase64;
    }

    public void setSeller(Seller seller) {
        this.seller = seller;
    }

    public Seller getSeller() {
        return seller;
    }

    // فقط برای JSON، مقدار sellerId را برمی‌گرداند
    public Long getSellerId() {
        return sellerId;
    }

    // بارگذاری منوی رستوران از پایگاه داده
    public void loadMenu(Session session) {
        this.menu = new Menu(this.id, session);
    }

    public Menu getMenu() {
        return menu;
    }
}
