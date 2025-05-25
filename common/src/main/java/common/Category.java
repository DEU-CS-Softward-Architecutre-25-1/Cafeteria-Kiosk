package common;

import java.util.List;

public record Category(String cateId, 
                       String cateName, 
                       List<Menu> menuList
                      ) {
}
