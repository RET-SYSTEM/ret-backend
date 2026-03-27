package me.cbhud.ret.controller;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import me.cbhud.ret.dto.response.CategoryResponse;
import me.cbhud.ret.entity.Category;
import me.cbhud.ret.repository.CategoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryRepository categoryRepository;

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAll() {
        List<CategoryResponse> categories = categoryRepository.findAllByOrderByNameAsc()
                .stream()
                .map(c -> new CategoryResponse(c.getId(), c.getName()))
                .toList();
        return ResponseEntity.ok(categories);
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> create(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Category name is required.");
        }

        if (categoryRepository.findByNameIgnoreCase(name).isPresent()) {
            throw new IllegalArgumentException("Category '" + name + "' already exists.");
        }

        Category saved = categoryRepository.save(Category.builder().name(name.trim()).build());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CategoryResponse(saved.getId(), saved.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new IllegalArgumentException("Category with id " + id + " not found.");
        }
        categoryRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
