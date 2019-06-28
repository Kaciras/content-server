package net.kaciras.blog.api.category;

import lombok.RequiredArgsConstructor;
import net.kaciras.blog.infrastructure.principal.RequireAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/categories")
class CategoryController {

	private final CategoryRepository repository;
	private final CategoryMapper mapper;

	@GetMapping("/{id}")
	public CategoryVo get(@PathVariable int id, @RequestParam(defaultValue = "false") boolean aggregate) {
		var category = repository.get(id);
		return aggregate ? mapper.aggregatedView(category) : mapper.categoryView(category);
	}

	@GetMapping("/{id}/children")
	public List<CategoryVo> getChildren(@PathVariable int id) {
		return mapper.categoryView(repository.get(id).getChildren());
	}

	@RequireAuthorize
	@PostMapping
	public ResponseEntity<CategoryVo> create(@RequestBody CategoryAttributes attrs, @RequestParam int parent) {
		var category = mapper.toCategory(attrs);
		repository.add(category, parent);
		return ResponseEntity
				.created(URI.create("/categories/" + category.getId()))
				.body(mapper.categoryView(category));
	}

	@RequireAuthorize
	@Transactional
	@PostMapping("/transfer")
	public ResponseEntity<Void> move(@RequestParam int id, @RequestParam int parent, @RequestParam boolean treeMode) {
		var category = repository.get(id);
		var newParent = repository.get(parent);

		if (treeMode) {
			category.moveTreeTo(newParent);
		} else {
			category.moveTo(newParent);
		}
		return ResponseEntity.noContent().build();
	}

	@RequireAuthorize
	@PutMapping("/{id}")
	public CategoryVo update(@PathVariable int id, @RequestBody CategoryAttributes attributes) {
		var category = repository.get(id);
		mapper.update(category, attributes);
		repository.update(category);
		return mapper.categoryView(category);
	}

// TODO:暂不支持删除，删除后文章的迁移有问题
//	@RequireAuthorize
//	@DeleteMapping("/{id}")
//	public ResponseEntity<Void> delete(@PathVariable int id, @RequestParam boolean tree) {
//		if (tree) {
//			repository.removeTree(id);
//		} else {
//			repository.remove(id);
//		}
//		return ResponseEntity.noContent().build();
//	}
}
