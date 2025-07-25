package org.example.Restaurant;

import org.example.Models.MenuItemDto;

import java.util.List;
import java.util.stream.Collectors;

public class MenuCategoryDTO {
    private Long id;
    private String title;
    private List<MenuItemDto> items;

    public MenuCategoryDTO(MenuCategory category) {
        this.id = category.getId();
        this.title = category.getTitle();
        this.items = category.getItems()
                .stream()
                .map(MenuItemDto::new)
                .collect(Collectors.toList());
    }
}