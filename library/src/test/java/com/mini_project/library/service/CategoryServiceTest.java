package com.mini_project.library.service;

import com.mini_project.library.dto.request.CategoryRequest;
import com.mini_project.library.dto.response.CategoryResponse;
import com.mini_project.library.entity.Category;
import com.mini_project.library.exception.DuplicateResourceException;
import com.mini_project.library.exception.ResourceNotFoundException;
import com.mini_project.library.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock CategoryRepository categoryRepository;

    @InjectMocks CategoryService categoryService;

    private Category sampleCategory;
    private CategoryRequest sampleRequest;

    @BeforeEach
    void setUp() {
        sampleCategory = Category.builder()
            .id(1L).name("Science Fiction").description("Sci-fi genre books").build();

        sampleRequest = new CategoryRequest();
        sampleRequest.setName("Science Fiction");
        sampleRequest.setDescription("Sci-fi genre books");
    }

    @Nested
    @DisplayName("getAllCategories")
    class GetAllCategories {

        @Test
        @DisplayName("returns paginated list of all categories")
        void returnsPage_ofAllCategories() {
            Pageable pageable = PageRequest.of(0, 20);
            given(categoryRepository.findAll(pageable))
                .willReturn(new PageImpl<>(List.of(sampleCategory)));

            var result = categoryService.getAllCategories(pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("Science Fiction");
        }
    }

    @Nested
    @DisplayName("getCategoryById")
    class GetCategoryById {

        @Test
        @DisplayName("returns CategoryResponse when category exists")
        void returnsCategory_whenFound() {
            given(categoryRepository.findById(1L)).willReturn(Optional.of(sampleCategory));

            CategoryResponse result = categoryService.getCategoryById(1L);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Science Fiction");
            assertThat(result.getDescription()).isEqualTo("Sci-fi genre books");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when category does not exist")
        void throwsNotFound_whenMissing() {
            given(categoryRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.getCategoryById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category");
        }
    }

    @Nested
    @DisplayName("createCategory")
    class CreateCategory {

        @Test
        @DisplayName("saves and returns new category when name is unique")
        void createsCategory_whenNameIsUnique() {
            given(categoryRepository.existsByNameIgnoreCase("Science Fiction")).willReturn(false);
            given(categoryRepository.save(any(Category.class))).willReturn(sampleCategory);

            CategoryResponse result = categoryService.createCategory(sampleRequest);

            assertThat(result.getName()).isEqualTo("Science Fiction");
            then(categoryRepository).should().save(any(Category.class));
        }

        @Test
        @DisplayName("throws DuplicateResourceException when category name already exists")
        void throwsDuplicate_whenNameAlreadyExists() {
            given(categoryRepository.existsByNameIgnoreCase("Science Fiction")).willReturn(true);

            assertThatThrownBy(() -> categoryService.createCategory(sampleRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Category");

            then(categoryRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateCategory")
    class UpdateCategory {

        @Test
        @DisplayName("updates description without duplicate check when name is unchanged")
        void updatesCategory_whenNameIsUnchanged() {
            given(categoryRepository.findById(1L)).willReturn(Optional.of(sampleCategory));
            given(categoryRepository.save(sampleCategory)).willReturn(sampleCategory);

            CategoryRequest request = new CategoryRequest();
            request.setName("Science Fiction");
            request.setDescription("Updated description");

            assertThatCode(() -> categoryService.updateCategory(1L, request))
                .doesNotThrowAnyException();

            assertThat(sampleCategory.getDescription()).isEqualTo("Updated description");
        }

        @Test
        @DisplayName("updates name when new name is unique")
        void updatesCategory_whenNewNameIsUnique() {
            given(categoryRepository.findById(1L)).willReturn(Optional.of(sampleCategory));
            given(categoryRepository.existsByNameIgnoreCase("Fantasy")).willReturn(false);
            given(categoryRepository.save(sampleCategory)).willReturn(sampleCategory);

            CategoryRequest request = new CategoryRequest();
            request.setName("Fantasy");
            request.setDescription("Fantasy genre books");

            categoryService.updateCategory(1L, request);

            assertThat(sampleCategory.getName()).isEqualTo("Fantasy");
        }

        @Test
        @DisplayName("throws DuplicateResourceException when new name is already taken")
        void throwsDuplicate_whenNewNameAlreadyTaken() {
            given(categoryRepository.findById(1L)).willReturn(Optional.of(sampleCategory));
            given(categoryRepository.existsByNameIgnoreCase("Fantasy")).willReturn(true);

            CategoryRequest request = new CategoryRequest();
            request.setName("Fantasy");

            assertThatThrownBy(() -> categoryService.updateCategory(1L, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Category");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when category does not exist")
        void throwsNotFound_whenMissing() {
            given(categoryRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.updateCategory(99L, sampleRequest))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteCategory")
    class DeleteCategory {

        @Test
        @DisplayName("deletes category when it exists")
        void deletesCategory_whenFound() {
            given(categoryRepository.findById(1L)).willReturn(Optional.of(sampleCategory));
            willDoNothing().given(categoryRepository).delete(sampleCategory);

            assertThatCode(() -> categoryService.deleteCategory(1L)).doesNotThrowAnyException();
            then(categoryRepository).should().delete(sampleCategory);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when category does not exist")
        void throwsNotFound_whenMissing() {
            given(categoryRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.deleteCategory(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category");
        }
    }
}
