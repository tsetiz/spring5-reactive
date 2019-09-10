package guru.springframework.services;

import guru.springframework.commands.IngredientCommand;
import guru.springframework.converters.IngredientCommandToIngredient;
import guru.springframework.converters.IngredientToIngredientCommand;
import guru.springframework.domain.Ingredient;
import guru.springframework.domain.Recipe;
import guru.springframework.repositories.reactive.RecipeReactiveRepository;
import guru.springframework.repositories.reactive.UnitOfMeasureReactiveRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Slf4j
@Service
public class IngredientServiceImpl implements IngredientService {

    private final RecipeReactiveRepository recipeReactiveRepository;
    private final IngredientToIngredientCommand ingredientToIngredientCommand;
    private final IngredientCommandToIngredient ingredientCommandToIngredient;
    private final UnitOfMeasureReactiveRepository unitOfMeasureReactiveRepository;

    public IngredientServiceImpl(RecipeReactiveRepository recipeReactiveRepository, IngredientToIngredientCommand ingredientToIngredientCommand,
                                 IngredientCommandToIngredient ingredientCommandToIngredient, UnitOfMeasureReactiveRepository unitOfMeasureReactiveRepository) {
        this.recipeReactiveRepository = recipeReactiveRepository;
        this.ingredientToIngredientCommand = ingredientToIngredientCommand;
        this.ingredientCommandToIngredient = ingredientCommandToIngredient;
        this.unitOfMeasureReactiveRepository = unitOfMeasureReactiveRepository;
    }

    @Override
    public Mono<IngredientCommand> findByRecipeIdAndIngredientId(String recipeId, String ingredientId) {
        // it would be simpler in case we just search an ingredient by id with the help of IngredientRepository
        // using findByRecipeIdAndId method.

        return recipeReactiveRepository.findById(recipeId)
                .flatMapIterable(Recipe::getIngredients)
                .filter(ingredient -> ingredient.getId().equalsIgnoreCase(ingredientId))
                .single()
                .map(ingredient -> {
                    IngredientCommand ingredientCommand = ingredientToIngredientCommand.convert(ingredient);
                    ingredientCommand.setRecipeId(recipeId);
                    return ingredientCommand;
                });
    }

    @Override
    public Mono<IngredientCommand> saveIngredientCommand(IngredientCommand command) {
        Recipe recipe = recipeReactiveRepository.findById(command.getRecipeId()).block();
        if (recipe == null) {
            //todo impl error handling
            log.error("recipe id not found. Id: " + command.getId());
            return Mono.just(new IngredientCommand());
        } else {
            Optional<Ingredient> optionalIngredient = recipe
                    .getIngredients()
                    .stream()
                    .filter(ingredient -> ingredient.getId().equals(command.getId()))
                    .findFirst();
            if (optionalIngredient.isPresent()) {
                Ingredient ingredientFound = optionalIngredient.get();
                ingredientFound.setDescription(command.getDescription());
                ingredientFound.setAmount(command.getAmount());
                ingredientFound.setUom(unitOfMeasureReactiveRepository.findById(command.getUom().getId()).block());
                //   .orElseThrow(() -> new RuntimeException("uom not found"))); //todo address this
                if (ingredientFound.getUom() == null) {
                    new RuntimeException("UOM NOT FOUND");
                }
            } else {
                //add new Ingredient
                Ingredient ingredient = ingredientCommandToIngredient.convert(command);
                recipe.addIngredient(ingredient);
            }
            Recipe savedRecipe = recipeReactiveRepository.save(recipe).block();

            Optional<Ingredient> savedIngredientOptional = savedRecipe.getIngredients().stream()
                    .filter(recipeIngredients -> recipeIngredients.getId().equals(command.getId()))
                    .findFirst();

            //check by description
            if (!savedIngredientOptional.isPresent()) {
                //not totally safe... But best guess
                savedIngredientOptional = savedRecipe.getIngredients().stream()
                        .filter(recipeIngredients -> recipeIngredients.getDescription().equals(command.getDescription()))
                        .filter(recipeIngredients -> recipeIngredients.getAmount().equals(command.getAmount()))
                        .filter(recipeIngredients -> recipeIngredients.getUom().getId().equals(command.getUom().getId()))
                        .findFirst();
            }

            //todo check for fail
            //enhance with id value
            IngredientCommand ingredientCommandSaved = ingredientToIngredientCommand.convert(savedIngredientOptional.get());
            ingredientCommandSaved.setRecipeId(recipe.getId());

            return Mono.just(ingredientCommandSaved);
        }
    }

    @Override
    public Mono<Void> deleteById(String recipeId, String id) {

        Recipe recipe = recipeReactiveRepository.findById(recipeId).block();
        if (recipe != null) {
            log.debug("recipe found.Id: " + recipeId);
            Optional<Ingredient> optionalIngredient = recipe
                    .getIngredients()
                    .stream()
                    .filter(ingredient -> ingredient.getId().equals(id))
                    .findFirst();
            if (optionalIngredient.isPresent()) {
                Ingredient ingredient = optionalIngredient.get();
                recipe.getIngredients().remove(ingredient);
                recipeReactiveRepository.save(recipe).block();
            } else {
                log.error("ingredient not found.Id: " + id);
            }
        } else {
            log.error("recipe id not found.Id: " + recipeId);
        }
        return Mono.empty();
    }
}
