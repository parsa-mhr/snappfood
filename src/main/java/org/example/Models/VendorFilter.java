package org.example.Models;

import java.util.List;

// DTO for filtering vendors
public class VendorFilter {
    private String search;
    private List<String> keywords;

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }
}
