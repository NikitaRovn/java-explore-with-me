package ru.practicum.main.category;

public final class CategoryMapper {
    private CategoryMapper() {
    }

    public static CategoryDto toDto(Category category) {
        if (category == null) {
            return null;
        }
        return new CategoryDto(category.getId(), category.getName());
    }

    public static Category toEntity(NewCategoryDto dto) {
        if (dto == null) {
            return null;
        }
        return new Category(null, dto.getName());
    }
}