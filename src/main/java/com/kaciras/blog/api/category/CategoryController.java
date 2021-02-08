package com.kaciras.blog.api.category;

import com.kaciras.blog.infra.principal.RequirePermission;
import lombok.RequiredArgsConstructor;
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
	public CategoryVO get(@PathVariable int id, @RequestParam(defaultValue = "false") boolean aggregate) {
		var category = repository.get(id);
		return aggregate ? mapper.aggregatedView(category) : mapper.categoryView(category);
	}

	@GetMapping("/{id}/children")
	public List<CategoryVO> getChildren(@PathVariable int id) {
		return mapper.categoryView(repository.get(id).getChildren());
	}

	@RequirePermission
	@PostMapping
	public ResponseEntity<CategoryVO> create(@RequestBody CategoryAttributes attrs, @RequestParam int parent) {
		var category = mapper.toCategory(attrs);
		repository.add(category, parent);

		return ResponseEntity
				.created(URI.create("/categories/" + category.getId()))
				.body(mapper.categoryView(category));
	}

	@RequirePermission
	@Transactional
	@PostMapping("/transfer")
	public ResponseEntity<Void> move(@RequestBody MoveDTO dto) {
		var category = repository.get(dto.id);
		var newParent = repository.get(dto.parent);

		if (dto.treeMode) {
			category.moveTreeTo(newParent);
		} else {
			category.moveTo(newParent);
		}

		return ResponseEntity.noContent().build();
	}

	@RequirePermission
	@PutMapping("/{id}")
	public CategoryVO update(@PathVariable int id, @RequestBody CategoryAttributes attributes) {
		var category = repository.get(id);
		mapper.update(category, attributes);
		repository.update(category);
		return mapper.categoryView(category);
	}

// TODO:暂不支持删除，删除后文章的迁移有问题
//	@RequirePermission
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
