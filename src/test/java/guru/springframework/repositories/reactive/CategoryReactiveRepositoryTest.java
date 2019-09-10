package guru.springframework.repositories.reactive;

import guru.springframework.domain.Category;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@DataMongoTest
public class CategoryReactiveRepositoryTest {

    @Autowired
    CategoryReactiveRepository categoryReactiveRepository;

    @Before
    public void setUp() {
        categoryReactiveRepository.deleteAll().block();
    }

    @Test
    public void testSaveCategory() {
        Category category = new Category();
        category.setId("1");

        categoryReactiveRepository.save(category).block();
        assertEquals("1", categoryReactiveRepository.count().block().toString());
    }

    @Test
    public void testFindByDescription() {
        Category category = new Category();
        category.setDescription("XYZ");
        categoryReactiveRepository.save(category).then().block();

        Category savedCategory = categoryReactiveRepository.findByDescription("XYZ").block();
        assertNotNull(savedCategory.getId());
        assertEquals("XYZ", savedCategory.getDescription());
    }
}
