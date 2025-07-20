package org.example.Restaurant;

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
    private Long id; // شناسه یکتا برای رستوران

    @Column(nullable = false)
    private String name; // نام رستوران (اجباری)

    @Column(nullable = false)
    private String address; // آدرس رستوران (اجباری)

    @Column(unique = true, nullable = false)
    private String phone; // شماره تلفن رستوران (اجباری و یکتا)

    @Column(name = "tax_fee", nullable = false)
    private Integer tax_fee; // هزینه مالیات رستوران (اجباری)

    @Column(name = "additional_fee", nullable = false)
    private Integer additional_fee; // هزینه اضافی رستوران (اجباری)

    @ElementCollection
    @Column(name = "working_hours")
    private List<String> workingHours = new ArrayList<>(); // ساعات کاری رستوران (اختیاری)

    @ElementCollection
    @Column(name = "categories")
    private List<String> categories = new ArrayList<>(); // دسته‌بندی‌های رستوران (اختیاری)

    @Lob
    @Column(name = "logo", columnDefinition = "LONGBLOB")
    private byte[] logo; // لوگوی رستوران به‌صورت باینری

    @ManyToOne
    @JoinColumn(name = "seller_id", nullable = false)
    private Seller seller; // فروشنده مرتبط با رستوران

    @Transient
    private String logoBase64; // لوگوی رستوران به‌صورت رشته Base64 برای پاسخ‌های API

    @Transient
    private Menu menu; // منوی رستوران که به‌صورت پویا بارگذاری می‌شود

    /**
     * سازنده پیش‌فرض
     */
    public Restaurant() {
    }

    /**
     * سازنده با پارامترهای اصلی
     * @param name نام رستوران
     * @param address آدرس رستوران
     * @param phone شماره تلفن رستوران
     * @param tax_fee هزینه مالیات
     * @param additional_fee هزینه اضافی
     * @param seller فروشنده مالک رستوران
     */
    public Restaurant(String name, String address, String phone, Integer tax_fee, Integer additional_fee, Seller seller) {
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.tax_fee = tax_fee;
        this.additional_fee = additional_fee;
        this.seller = seller;
    }

    /**
     * دریافت شناسه رستوران
     * @return شناسه رستوران
     */
    public Long getId() {
        return id;
    }

    /**
     * تنظیم نام رستوران
     * @param name نام جدید
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * دریافت نام رستوران
     * @return نام رستوران
     */
    public String getName() {
        return name;
    }

    /**
     * تنظیم آدرس رستوران
     * @param address آدرس جدید
     */
    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * دریافت آدرس رستوران
     * @return آدرس رستوران
     */
    public String getAddress() {
        return address;
    }

    /**
     * تنظیم شماره تلفن رستوران
     * @param phone شماره تلفن جدید
     */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * دریافت شماره تلفن رستوران
     * @return شماره تلفن رستوران
     */
    public String getPhone() {
        return phone;
    }

    /**
     * تنظیم هزینه مالیات
     * @param tax_fee هزینه مالیات جدید
     */
    public void setTaxFee(Integer tax_fee) {
        this.tax_fee = tax_fee;
    }

    /**
     * دریافت هزینه مالیات
     * @return هزینه مالیات
     */
    public Integer getTaxFee() {
        return tax_fee;
    }

    /**
     * تنظیم هزینه اضافی
     * @param additional_fee هزینه اضافی جدید
     */
    public void setAdditionalFee(Integer additional_fee) {
        this.additional_fee = additional_fee;
    }

    /**
     * دریافت هزینه اضافی
     * @return هزینه اضافی
     */
    public Integer getAdditionalFee() {
        return additional_fee;
    }

    /**
     * تنظیم ساعات کاری
     * @param workingHours لیست ساعات کاری
     */
    public void setWorkingHours(List<String> workingHours) {
        this.workingHours = workingHours;
    }

    /**
     * دریافت ساعات کاری
     * @return لیست ساعات کاری
     */
    public List<String> getWorkingHours() {
        return workingHours;
    }

    /**
     * تنظیم دسته‌بندی‌ها
     * @param categories لیست دسته‌بندی‌ها
     */
    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    /**
     * دریافت دسته‌بندی‌ها
     * @return لیست دسته‌بندی‌ها
     */
    public List<String> getCategories() {
        return categories;
    }

    /**
     * تنظیم لوگوی رستوران
     * @param logo لوگو به‌صورت باینری
     */
    public void setLogo(byte[] logo) {
        this.logo = logo;
        this.logoBase64 = (logo != null) ? Base64.getEncoder().encodeToString(logo) : null;
    }

    /**
     * دریافت لوگوی رستوران
     * @return لوگو به‌صورت باینری
     */
    public byte[] getLogo() {
        return logo;
    }

    /**
     * تنظیم لوگوی Base64
     * @param logoBase64 لوگو به‌صورت رشته Base64
     */
    public void setLogoBase64(String logoBase64) {
        this.logoBase64 = logoBase64;
        this.logo = (logoBase64 != null) ? Base64.getDecoder().decode(logoBase64) : null;
    }

    /**
     * دریافت لوگوی Base64
     * @return لوگو به‌صورت رشته Base64
     */
    public String getLogoBase64() {
        return logoBase64;
    }

    /**
     * تنظیم فروشنده رستوران
     * @param seller فروشنده جدید
     */
    public void setSeller(Seller seller) {
        this.seller = seller;
    }

    /**
     * دریافت فروشنده رستوران
     * @return فروشنده رستوران
     */
    public Seller getSeller() {
        return seller;
    }

    /**
     * بارگذاری منوی رستوران از پایگاه داده
     * @param session سشن Hibernate
     */
    public void loadMenu(Session session) {
        this.menu = new Menu(this.id, session);
    }

    /**
     * دریافت منوی رستوران
     * @return منوی رستوران
     */
    public Menu getMenu() {
        return menu;
    }
}
