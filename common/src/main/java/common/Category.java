package common;

import java.util.List;

public record Category(String cateId, String cateName, List<Menu> menuList) {
    public String getcateId() { return cateId; }
    public String getcateName() { return cateName; }
    public List<Menu> menuList() { return menuList; }
}
