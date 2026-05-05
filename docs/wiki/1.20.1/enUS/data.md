# Data

PiDataGraph reads Minecraft datapack JSON. Fields can use normal Codec-backed types or expression types such as `PiDoubleExpression`, `PiIntExpression`, and `PiBooleanExpression`. Expressions are compiled during load; runtime code only provides the context for the current event.

## Define a Data Object

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

Expression fields stay readable in JSON while Java keeps a typed model.

## Declare the Data Type

`PiDataDefinition` declares the data id, folder, Codec, and load-time validation.

```java
public static final PiDataDefinition<SpellFormula> SPELL_FORMULA_DATA =
        PiDataDefinition.<SpellFormula>builder(
                new ResourceLocation("examplemod", "spell_formulas"),
                "spell_formulas",
                SpellFormula.CODEC)
        .verify("damage", (context, spec) ->
                spec.damage().compile(context.expressionLanguage(), context.expressionScope()))
        .verify("cooldown", (context, spec) ->
                spec.cooldown().compile(context.expressionLanguage(), context.expressionScope()))
        .verify("enabled", (context, spec) ->
                spec.enabled().compile(context.expressionLanguage(), context.expressionScope()))
        .build();
```

`folder = "spell_formulas"` reads:

```text
data/<namespace>/spell_formulas/<path>.json
```

## Compile a Runtime Library

Use `PiEngineContentType` when raw data should become precompiled runtime content.

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
public static final PiEngineContentType<SpellFormula, CompiledSpellFormula> SPELL_FORMULAS =
        PiEngineContentType.builder(SPELL_FORMULA_DATA, (entry, context) -> new CompiledSpellFormula(
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

At runtime, evaluate compiled expressions against `PiEngineContext`.

```java
CompiledSpellFormula fireball = formulas.require(new ResourceLocation("examplemod", "fireball"));

double damage = context.evaluate(fireball.damage());
int cooldown = context.evaluate(fireball.cooldown());
boolean enabled = context.evaluate(fireball.enabled());
```

## Server Reload Hook

Register an `AddReloadListenerEvent` listener for server-side datapack data. It runs when the server loads a world and when `/reload` runs.

```java
@Mod.EventBusSubscriber(modid = ExampleMod.MODID)
public final class ExampleDataReloads {
    private static volatile PiEngineLibrary<CompiledSpellFormula> formulas =
            PiEngineLibrary.<CompiledSpellFormula>builder().build();

    private ExampleDataReloads() {
    }

    @SubscribeEvent
    public static void addReloadListeners(AddReloadListenerEvent event) {
        PiEngineBuildContext buildContext = PiEngineBuildContext.standard()
                .withScope(SPELL_FORMULAS.scope());

        event.addListener(new PiDataReloadListener<>(SPELL_FORMULA_DATA, buildContext.dataContext()) {
            @Override
            protected void applyData(
                    PiDataSet<SpellFormula> data,
                    ResourceManager resourceManager,
                    ProfilerFiller profiler
            ) {
                formulas = PiEngineLibrary.compile(data, buildContext, SPELL_FORMULAS);
            }
        });
    }

    public static CompiledSpellFormula requireFormula(ResourceLocation id) {
        return formulas.require(id);
    }
}
```

`AddReloadListenerEvent` reads JSON from enabled datapacks. Use `AddPackFindersEvent` only when the mod also needs to expose a built-in optional datapack.

## Datagen

Use `PiDataCatalog` to write entries and `PiDataProvider` to emit JSON. Pass a validation context to catch expression mistakes during datagen.

```java
PiDataSet<SpellFormula> spells = PiDataCatalog.builder(SPELL_FORMULA_DATA, "examplemod")
        .add(builder -> builder.entry("fireball", new SpellFormula(
                PiDoubleExpression.of("baseDamage + spellPower * 2 - targetArmor"),
                PiIntExpression.of("max(5, baseCooldown - haste)"),
                PiBooleanExpression.of("mana >= cost & cooldownReady == 1"))))
        .buildSet();

gen.addProvider(server, new PiDataProvider(
        output,
        "Example Spell Formulas",
        PiEngineBuildContext.standard()
                .withScope(SPELL_FORMULAS.scope())
                .dataContext(),
        spells));
```

Output path:

```text
data/examplemod/spell_formulas/fireball.json
```

## Expression Syntax

| Kind | Contents |
| --- | --- |
| Math | `+`, `-`, `*`, `/`, `%`, `^` |
| Comparison | `<`, `<=`, `>`, `>=`, `==`, `!=` |
| Logic | `&`, `|` |
| Functions | `min`, `max`, `clamp`, `abs`, `floor`, `ceil`, `round`, `sqrt`, `pow`, `sin`, `cos`, `tan`, `rand` |

Custom function:

```java
PiExpressionLanguage language = PiExpressionLanguage.standardBuilder()
        .function("scale_by_power", 2, (context, args) ->
                args[0] * context.variables().get("spellPower") + args[1])
        .build();
```

Use the same language in build context and runtime context.

## Error Paths

Validation errors include the data id and field path:

```text
examplemod:fireball/damage: invalid expression `baseDamage + missing` for variables [baseDamage, spellPower, targetArmor]
```

This catches broken data during reload instead of failing later during a spell cast or machine tick.
