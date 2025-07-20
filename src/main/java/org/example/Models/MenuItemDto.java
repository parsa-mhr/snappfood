package org.example.Models;

import java.util.List;

public class MenuItemDto {
    public long id;
    public String name;
    public String imageBase64;
    public String description;
    public int vendor_id;
    public int price;
    public int supply;
    public List<String> keywords;

    public MenuItemDto() {}

    public MenuItemDto(long id, String name, String imageBase64, String description, int vendor_id, int price, int supply, List<String> keywords) {
        this.id = id;
        this.name = name;
        this.imageBase64 = imageBase64;
        this.description = description;
        this.vendor_id = vendor_id;
        this.price = price;
        this.supply = supply;
        this.keywords = keywords;
    }
}

