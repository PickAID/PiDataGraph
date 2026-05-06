package org.pickaid.pidatagraph.engine.predicate;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import org.pickaid.pidatagraph.data.PiDataBuildContext;
import org.pickaid.pidatagraph.data.PiDataVerificationException;
import org.pickaid.pidatagraph.engine.context.PiEngineContextContract;
import org.pickaid.pidatagraph.expression.PiBooleanExpression;
import org.pickaid.pidatagraph.expression.PiDoubleExpression;

public final class PiEnginePredicates {
    public static final PiEnginePredicateType<PiExpressionPredicate> EXPRESSION = PiEnginePredicateType.of(
            id("expression"),
            ignored -> PiExpressionPredicate.CODEC
    );
    public static final PiEnginePredicateType<PiAllPredicate> ALL = PiEnginePredicateType.of(
            id("all"),
            PiAllPredicate::codec
    );
    public static final PiEnginePredicateType<PiAnyPredicate> ANY = PiEnginePredicateType.of(
            id("any"),
            PiAnyPredicate::codec
    );
    public static final PiEnginePredicateType<PiNotPredicate> NOT = PiEnginePredicateType.of(
            id("not"),
            PiNotPredicate::codec
    );
    public static final PiEnginePredicateType<PiChancePredicate> CHANCE = PiEnginePredicateType.of(
            id("chance"),
            ignored -> PiChancePredicate.CODEC
    );
    public static final PiEnginePredicateType<PiHasObjectPredicate> HAS_OBJECT = PiEnginePredicateType.of(
            id("has_object"),
            ignored -> PiHasObjectPredicate.CODEC
    );
    public static final PiEnginePredicateType<PiHasNumberPredicate> HAS_NUMBER = PiEnginePredicateType.of(
            id("has_number"),
            ignored -> PiHasNumberPredicate.CODEC
    );
    public static final PiEnginePredicateType<PiNumberRangePredicate> NUMBER_RANGE = PiEnginePredicateType.of(
            id("number_range"),
            ignored -> PiNumberRangePredicate.CODEC
    );
    public static final PiEnginePredicateType<PiObjectEqualsPredicate> OBJECT_EQUALS = PiEnginePredicateType.of(
            id("object_equals"),
            ignored -> PiObjectEqualsPredicate.CODEC
    );
    public static final PiEnginePredicateType<PiEntityTypePredicate> ENTITY_TYPE = PiEnginePredicateType.of(
            id("entity_type"),
            ignored -> PiEntityTypePredicate.CODEC
    );
    public static final PiEnginePredicateType<PiLevelDimensionPredicate> LEVEL_DIMENSION = PiEnginePredicateType.of(
            id("level_dimension"),
            ignored -> PiLevelDimensionPredicate.CODEC
    );
    public static final PiEnginePredicateType<PiBlockStatePredicate> BLOCK_STATE = PiEnginePredicateType.of(
            id("block_state"),
            ignored -> PiBlockStatePredicate.CODEC
    );
    public static final PiEnginePredicateType<PiBiomePredicate> BIOME = PiEnginePredicateType.of(
            id("biome"),
            ignored -> PiBiomePredicate.CODEC
    );
    public static final PiEnginePredicateType<PiItemEnchantmentPredicate> ITEM_ENCHANTMENT = PiEnginePredicateType.of(
            id("item_enchantment"),
            ignored -> PiItemEnchantmentPredicate.CODEC
    );
    public static final PiEnginePredicateType<PiItemStackPredicate> ITEM_STACK = PiEnginePredicateType.of(
            id("item_stack"),
            ignored -> PiItemStackPredicate.CODEC
    );

    private PiEnginePredicates() {
    }

    public static List<PiEnginePredicateType<? extends PiEnginePredicate>> core() {
        return List.of(
                EXPRESSION, ALL, ANY, NOT, CHANCE, HAS_OBJECT, HAS_NUMBER, NUMBER_RANGE, OBJECT_EQUALS,
                ENTITY_TYPE, LEVEL_DIMENSION, BLOCK_STATE, BIOME, ITEM_ENCHANTMENT, ITEM_STACK);
    }

    public static Codec<PiEnginePredicate> standardCodec() {
        return PiEnginePredicateRegistry.builder().install(core()).build().codec();
    }

    public static void verify(String path, PiDataBuildContext context, PiBooleanExpression expression) {
        try {
            expression.compile(context.expressionLanguage(), context.expressionScope());
        } catch (RuntimeException error) {
            throw new PiDataVerificationException(path, error);
        }
    }

    public static void verify(String path, PiDataBuildContext context, PiDoubleExpression expression) {
        try {
            expression.compile(context.expressionLanguage(), context.expressionScope());
        } catch (RuntimeException error) {
            throw new PiDataVerificationException(path, error);
        }
    }

    public static PiEngineContextContract contextContract(String owner, PiEnginePredicate predicate) {
        String checkedOwner = Objects.requireNonNull(owner, "owner");
        PiEnginePredicate checkedPredicate = Objects.requireNonNull(predicate, checkedOwner + " predicate");
        PiEnginePredicateType<?> type = Objects.requireNonNull(checkedPredicate.type(), checkedOwner + " predicate returned null type");
        return Objects.requireNonNull(
                checkedPredicate.contextContract(),
                checkedOwner + " predicate " + type.id() + " returned null context contract"
        );
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation("pidatagraph", path);
    }
}
