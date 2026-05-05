# Data and Expressions

`PiDataDefinition` describes one JSON data type. Fields can be normal Codec-backed values or expression values.

## Data Model

```java
public record SpellFormula(
        PiDoubleExpression damage,
        PiIntExpression cooldown,
        PiBooleanExpression enabled
) {
    public static final Codec<SpellFormula> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PiDoubleExpression.CODEC.fieldOf("damage").forGetter(SpellFormula::damage),
            PiIntExpression.CODEC.fieldOf("cooldown").forGetter(SpellFormula::cooldown),
            PiBooleanExpression.CODEC.fieldOf("enabled").forGetter(SpellFormula::enabled)
    ).apply(instance, SpellFormula::new));
}
```

JSON:

```json
{
  "damage": "baseDamage + spellPower * 2 - targetArmor",
  "cooldown": "max(5, baseCooldown - haste)",
  "enabled": "mana >= cost & cooldownReady == 1"
}
```

## Define the Folder and Validation

```java
public static final PiDataDefinition<SpellFormula> SPELL_FORMULAS =
        PiDataDefinition.<SpellFormula>builder(
                new ResourceLocation("examplemod", "spell_formulas"),
                "spell_formulas",
                SpellFormula.CODEC)
        .verify("damage", (context, value) ->
                value.damage().compile(context.expressionLanguage(), context.expressionScope()))
        .verify("cooldown", (context, value) ->
                value.cooldown().compile(context.expressionLanguage(), context.expressionScope()))
        .verify("enabled", (context, value) ->
                value.enabled().compile(context.expressionLanguage(), context.expressionScope()))
        .build();
```

This reads:

```text
data/<namespace>/spell_formulas/<path>.json
```

## Compile a Runtime Library

```java
public record CompiledSpellFormula(
        ResourceLocation id,
        PiCompiledDoubleExpression damage,
        PiCompiledIntExpression cooldown,
        PiCompiledBooleanExpression enabled
) {
}
```

```java
public static final PiEngineContentType<SpellFormula, CompiledSpellFormula> COMPILED_SPELL_FORMULAS =
        PiEngineContentType.builder(SPELL_FORMULAS, (entry, context) -> new CompiledSpellFormula(
                entry.id(),
                entry.value().damage().compile(context.expressionLanguage(), context.expressionScope()),
                entry.value().cooldown().compile(context.expressionLanguage(), context.expressionScope()),
                entry.value().enabled().compile(context.expressionLanguage(), context.expressionScope())))
        .scope(PiExpressionScope.of(
                "baseDamage",
                "spellPower",
                "targetArmor",
                "baseCooldown",
                "haste",
                "mana",
                "cost",
                "cooldownReady"))
        .build();
```

```java
PiEngineLibrary<CompiledSpellFormula> formulas = PiEngineLibrary.compile(
        loadedSet,
        PiEngineBuildContext.standard(),
        COMPILED_SPELL_FORMULAS);
```

## Load Normal Datapack JSON

```java
event.addListener(new PiDataReloadListener<>(SPELL_FORMULAS, PiEngineBuildContext.standard()
        .withScope(COMPILED_SPELL_FORMULAS.scope())
        .dataContext()) {
    @Override
    protected void applyData(PiDataSet<SpellFormula> data, ResourceManager manager, ProfilerFiller profiler) {
        FORMULAS = PiEngineLibrary.compile(data, PiEngineBuildContext.standard(), COMPILED_SPELL_FORMULAS);
    }
});
```

## Datagen

```java
PiDataSet<SpellFormula> spells = PiDataCatalog.builder(SPELL_FORMULAS, "examplemod")
        .add(builder -> builder.entry("fireball", new SpellFormula(
                PiDoubleExpression.of("baseDamage + spellPower * 2 - targetArmor"),
                PiIntExpression.of("max(5, baseCooldown - haste)"),
                PiBooleanExpression.of("mana >= cost & cooldownReady == 1"))))
        .buildSet();

gen.addProvider(server, new PiDataProvider(
        output,
        "Example Spell Formulas",
        PiEngineBuildContext.standard()
                .withScope(COMPILED_SPELL_FORMULAS.scope())
                .dataContext(),
        spells));
```

## Expression Syntax

| Kind | Supported |
| --- | --- |
| Math | `+`, `-`, `*`, `/`, `%`, `^` |
| Comparison | `<`, `<=`, `>`, `>=`, `==`, `!=` |
| Logic | `&`, `|` |
| Functions | `min`, `max`, `clamp`, `abs`, `floor`, `ceil`, `round`, `sqrt`, `pow`, `sin`, `cos`, `tan`, `rand(min,max)` |

Register custom functions with `PiExpressionLanguage.standardBuilder()`. Use the same language in the build context and runtime context.
