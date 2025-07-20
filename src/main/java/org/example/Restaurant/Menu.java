package org.example.Restaurant;

import org.hibernate.Session;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * کلاس Menu برای مدیریت منوی رستوران
 * این کلاس شامل دسته‌بندی‌ها (menu_titles) و آیتم‌های منو است
 */
public class Menu {
    private List<String> menuTitles;
    private List<MenuItem> items;
    private Map<String, List<MenuItem>> itemsByCategory;

    public Menu(Long restaurantId, Session session) {
        this.menuTitles = new ArrayList<>();
        this.items = new ArrayList<>();
        this.itemsByCategory = new HashMap<>();

        String hqlItems = "FROM MenuItem m WHERE m.restaurant.id = :restaurantId";
        this.items = session.createQuery(hqlItems, MenuItem.class)
                .setParameter("restaurantId", restaurantId)
                .list();

        this.menuTitles = items.stream()
                .flatMap(item -> item.getKeywords().stream())
                .distinct()
                .collect(Collectors.toList());

        for (String keyword : menuTitles) {
            List<MenuItem> categoryItems = items.stream()
                    .filter(item -> item.getKeywords().contains(keyword))
                    .collect(Collectors.toList());
            itemsByCategory.put(keyword, categoryItems);
        }

        List<MenuItem> uncategorizedItems = items.stream()
                .filter(item -> item.getKeywords() == null || item.getKeywords().isEmpty())
                .collect(Collectors.toList());
        if (!uncategorizedItems.isEmpty()) {
            itemsByCategory.put("Uncategorized", uncategorizedItems);
            if (!menuTitles.contains("Uncategorized")) {
                menuTitles.add("Uncategorized");
            }
        }
    }

    public List<String> getMenuTitles() { return menuTitles; }
    public List<MenuItem> getItems() { return items; }
    public List<MenuItem> getItemsByCategory(String title) {
        return itemsByCategory.getOrDefault(title, new ArrayList<>());
    }

    public Map<String, Object> toJson(Restaurant restaurant) {
        Map<String, Object> json = new HashMap<>();
        json.put("vendor", Map.of(
                "id", restaurant.getId(),
                "name", restaurant.getName(),
                "address", restaurant.getAddress(),
                "phone", restaurant.getPhone(),
                "logoBase64", restaurant.getLogoBase64() != null ? restaurant.getLogoBase64() : "",
                "tax_fee", restaurant.getTaxFee(),
                "additional_fee", restaurant.getAdditionalFee()
        ));
        json.put("menu_titles", menuTitles);

        Map<String, List<Map<String, Object>>> itemsByTitle = new HashMap<>();
        for (String title : menuTitles) {
            List<Map<String, Object>> categoryItems = itemsByCategory.getOrDefault(title, new ArrayList<>())
                    .stream()
                    .map(item -> Map.of(
                            "id", item.getId(),
                            "name", item.getName(),
                            "description", item.getDescription(),
                            "price", item.getPrice(),
                            "supply", item.getSupply(),
                            "keywords", item.getKeywords() != null ? item.getKeywords() : new ArrayList<>(),
                            "vendor_id", restaurant.getId(),
                            "imageBase64", item.getImageBase64() != null ? item.getImageBase64() : ""
                    ))
                    .collect(Collectors.toList());
            itemsByTitle.put(title, categoryItems);
        }
        json.putAll(itemsByTitle);

        return json;
    }
}
