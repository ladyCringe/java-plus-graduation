package ru.practicum.event.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.event.entity.Category;
import ru.practicum.main.event.dto.CategoryDto;
import ru.practicum.event.dto.NewCategoryDto;

@UtilityClass
public class CategoryMapper {

    public static Category toEntity(NewCategoryDto dto) {
        return new Category(dto.getName());
    }

    public static CategoryDto toDto(Category category) {
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
    }

    public static Category updateFromDto(CategoryDto dto, Category category) {
        category.setName(dto.getName());
        return category;
    }
}