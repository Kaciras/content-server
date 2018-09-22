package net.kaciras.blog.api.category;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
interface CategoryMapper {

	CategoryVo categoryView(Category category);

	List<CategoryVo> categoryView(List<Category> histories);

	Category toCategory(CategoryAttributes viewObject);

	void update(CategoryAttributes attributes, @MappingTarget Category category);
}
