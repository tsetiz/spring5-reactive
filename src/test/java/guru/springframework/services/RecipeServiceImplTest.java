package guru.springframework.services;

import guru.springframework.commands.RecipeCommand;
import guru.springframework.converters.RecipeCommandToRecipe;
import guru.springframework.converters.RecipeToRecipeCommand;
import guru.springframework.domain.Recipe;
import guru.springframework.exceptions.NotFoundException;
import guru.springframework.repositories.RecipeRepository;
import guru.springframework.repositories.reactive.RecipeReactiveRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

public class RecipeServiceImplTest {

    private RecipeServiceImpl recipeService;

    @Mock
    RecipeReactiveRepository recipeReactiveRepository;

    @Mock
    RecipeToRecipeCommand recipeToRecipeCommand;

    @Mock
    RecipeCommandToRecipe recipeCommandToRecipe;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        recipeService = new RecipeServiceImpl(recipeReactiveRepository, recipeCommandToRecipe, recipeToRecipeCommand);
    }

    @Test
    public void getRecipes() {
        Recipe recipe = new Recipe();
        HashSet recipeSet = new HashSet();
        recipeSet.add(recipe);
        when(recipeReactiveRepository.findAll()).thenReturn(Flux.fromIterable(recipeSet));

        List<Recipe> recipes = recipeService.getRecipes().collectList().block();

        assertEquals(recipes.size(), 1);
        verify(recipeReactiveRepository, times(1)).findAll();

    }

    @Test
    public void findById() {
        Recipe recipe = new Recipe();
        recipe.setId("1");

        when(recipeReactiveRepository.findById(anyString())).thenReturn(Mono.just(recipe));
        Recipe recipeReturned = recipeService.findById("1").block();
        assertNotNull("Null Recipe returned", recipeReturned);

        verify(recipeReactiveRepository, times(1)).findById(anyString());
        verify(recipeReactiveRepository, never()).findAll();
    }

    @Test
    public void findCommandById() {
        Recipe recipe = new Recipe();
        recipe.setId("4");
        when(recipeReactiveRepository.findById(anyString())).thenReturn(Mono.just(recipe));

        RecipeCommand recipeCommand = new RecipeCommand();
        recipeCommand.setId("4");
        when(recipeToRecipeCommand.convert(any())).thenReturn(recipeCommand);

        RecipeCommand findCommand = recipeService.findCommandById("4").block();

        assertNotNull("Null Recipe returned", findCommand);
        verify(recipeReactiveRepository, times(1)).findById(anyString());
        assertEquals(recipe.getId(), findCommand.getId());
    }

    @Test
    public void deleteById() {
        String idToDelete = "2";
        when(recipeReactiveRepository.deleteById(anyString())).thenReturn(Mono.empty());
        recipeService.deleteById(idToDelete);
        verify(recipeReactiveRepository, times(1)).deleteById(anyString());
    }
}