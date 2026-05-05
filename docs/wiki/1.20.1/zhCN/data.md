# 数据和表达式

`PiDataDefinition` 描述一种 JSON 数据。字段可以是普通 Codec 类型，也可以是表达式类型。

## 数据模型

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

JSON：

```json
{
  "damage": "baseDamage + spellPower * 2 - targetArmor",
  "cooldown": "max(5, baseCooldown - haste)",
  "enabled": "mana >= cost & cooldownReady == 1"
}
```

## 定义 JSON 文件夹和校验

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

这会读取：

```text
data/<namespace>/spell_formulas/<path>.json
```

## 编译成运行时库

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

## 读取普通数据包 JSON

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

## 表达式语法

| 类型 | 支持内容 |
| --- | --- |
| 数学 | `+`, `-`, `*`, `/`, `%`, `^` |
| 比较 | `<`, `<=`, `>`, `>=`, `==`, `!=` |
| 逻辑 | `&`, `|` |
| 函数 | `min`, `max`, `clamp`, `abs`, `floor`, `ceil`, `round`, `sqrt`, `pow`, `sin`, `cos`, `tan`, `rand(min,max)` |

自定义函数通过 `PiExpressionLanguage.standardBuilder()` 注册，并在 build context 和 runtime context 中使用同一个 language。
