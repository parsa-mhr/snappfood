package org.example.Models;

import org.example.Restaurant.MenuItem;

import java.util.ArrayList;
import java.util.List;

public class MenuItemDto2 {
    public long id;
    public String name;
    public String imageBase64;
    public String description;
    public int vendor_id;
    public int price;
    public int supply;
    public List<String> keywords;

    public MenuItemDto2() {
        keywords = new ArrayList<String>() ;
    }

    public MenuItemDto2(long id, String name, String imageBase64, String description, int vendor_id, int price, int supply , List<String> keywords) {
        this.id = id;
        this.name = name;
        this.imageBase64 = imageBase64;
        this.description = description;
        this.vendor_id = vendor_id;
        this.price = price;
        this.supply = supply;
        this.keywords = keywords;
    }

    public MenuItemDto2(MenuItem menuItem) {
        this.id = menuItem.getId();
        this.name = menuItem.getName() ;
        this.price = menuItem.getPrice();
        this.supply = menuItem.getSupply();
        this.description = menuItem.getDescription();
        this.imageBase64 = menuItem.getImageBase64();
        this.vendor_id = Math.toIntExact(menuItem.getRestaurant().getId());
        this.keywords = menuItem.getKeywords() ;
    }
}

