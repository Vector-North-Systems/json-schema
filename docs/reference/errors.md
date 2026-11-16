# Errors

The processor reports what it can't describe as a build error, pointing at the property or the class. Values in
`<angle brackets>` change with your code.

## Types
These point at the property.

| Message                                                                                                   | Cause                                                              | Fix                                                                                   |
|-----------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------|---------------------------------------------------------------------------------------|
| `No schema for this property: Any could be anything; use a concrete type`                                 | The property is an `Any`, or holds one.                            | Use the type the value really has.                                                    |
| `No schema for this property: interfaces, sealed and abstract classes like <type> aren't supported yet`   | The property's type has subclasses the schema can't list yet.      | Use a concrete class. Sealed classes are on the [roadmap](../roadmap.md).             |
| `No schema for this property: The type parameter <T> has no type argument to describe; use a concrete type.` | A type parameter is used without a type argument.               | Use the generic class with type arguments, e.g. `Box<String>`.                        |
| `No schema for this property: <type> isn't a class`                                                       | The type isn't a class, e.g. a function type.                      | Use a class, or leave the property out with `@SchemaExclude`.                         |
| `No schema for this property: cannot resolve the element type of <type>`                                  | The element type of a collection can't be read, e.g. `List<*>`.    | Give the collection a concrete element type.                                          |
| `No schema for this property: cannot resolve the key and value types of <type>`                           | The types of a map can't be read, e.g. `Map<*, *>`.                | Give the map concrete types.                                                          |
| `No schema for this property: cannot find the underlying value of <type>`                                 | A value class has no property to describe it by.                   | Wrap a property with a backing field.                                                 |
| `Map keys must be String, found <key> in <type>`                                                          | A map has a key that isn't a `String`. JSON object keys are strings. | Use `Map<String, V>`, or a list of entries.                                         |
| `<type> is nested more than 32 objects deep`                                                              | Objects nest too deep, usually a generic type that grows without repeating, e.g. a `Box<T>` holding a `Box<List<T>>`. | Flatten the classes, or break the cycle. |

## Annotations
These point at the property.

| Message                                                                    | Cause                                                        | Fix                                              |
|----------------------------------------------------------------------------|--------------------------------------------------------------|--------------------------------------------------|
| `@SchemaFormat only applies to String properties`                          | The property isn't a `String`, or is an enum.                | Remove it, or make the property a `String`.      |
| `@SchemaMinimum and @SchemaMaximum only apply to number properties`        | The property isn't an integer or a number.                   | Remove them. For collections, use `@SchemaMinItems` and `@SchemaMaxItems`. |
| `@SchemaMinimum must be finite, found <value>`, and the same for `@SchemaMaximum` | The bound is `NaN` or an infinity.                    | Use a finite number, or remove the bound.        |
| `@SchemaMinimum(<min>) is greater than @SchemaMaximum(<max>)`              | No value fits between the bounds.                            | Swap or fix the bounds.                          |
| `@SchemaMinItems and @SchemaMaxItems only apply to collection properties`  | The property isn't a collection.                             | Remove them. For numbers, use `@SchemaMinimum` and `@SchemaMaximum`. |
| `@SchemaMinItems(<value>) can't be negative`, and the same for `@SchemaMaxItems` | The bound is negative.                                 | Use 0 or more.                                   |
| `@SchemaMinItems(<min>) is greater than @SchemaMaxItems(<max>)`            | No size fits between the bounds.                             | Swap or fix the bounds.                          |

## Roots
These point at the class with `@SchemaRoot`.

| Message                                                                                                                             | Cause                                                                                      | Fix                                                                 |
|-------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------|---------------------------------------------------------------------|
| `@SchemaRoot classes can't have type parameters`                                                                                    | The root is generic, so its schema depends on type arguments nobody gave.                  | Annotate a class without type parameters that uses the generic one. |
| `@SchemaRoot requires a concrete class or object; enums, interfaces, annotation classes, sealed and abstract classes aren't supported` | The root isn't a concrete class. Enum properties are still supported.                    | Annotate a concrete class, with the type as a property.             |
| `@SchemaRoot(name = "<name>") isn't a valid Kotlin identifier`                                                                      | The name can't be the name of an object.                                                   | Choose a name like `ComplexJsonSchema`.                             |
| `<name> is already generated for <class>, set a different @SchemaRoot(name)`                                                        | Two roots in a package get the same object name, or names that differ only in case, like `foo` and `Foo`, which would be written to the same file. | Set a different `name` on one of them. |
| `@SchemaRoot(targets) has an unknown target '<target>'. Valid targets are: [GEMINI, OPENAI]`                                        | The annotations are newer than the processor, which doesn't know the dialect.              | Use the same version of `json-schema-annotations` and `json-schema-ksp`. |

## Dialect errors
These come from the rules of one dialect, and are reported for every root that generates it. If you don't use the
dialect, leave it out; see [Choosing the dialects](targets.md#choosing-the-dialects).

### `OPENAI`
| Message                                                                                                                                                                         | Cause                                                             | Fix                                                                                            |
|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------|------------------------------------------------------------------------------------------------|
| `Map isn't supported by the OPENAI dialect, which requires every object to have fixed properties. Use a data class if the keys are known, or a List of key-value entries.`      | A property is a `Map`, or holds one. It points at the property.   | [Replace the map](../guides/openai.md#replacing-a-map), or don't generate `OPENAI` for the class. |

### `GEMINI`
| Message                                                                                                                                                                     | Cause                                                                                                             | Fix                                                                                   |
|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------|
| `A required property that leads back to its own class, <class>, isn't supported by the GEMINI dialect. Make it optional with a default: <property>: ... = null.`              | A nullable property without a default has the type of its own class, or leads back to it through required properties only, e.g. `val parent: Category?` in `Category`. Gemini rejects the schema. It points at the property. | Give the property a default, `val parent: Category? = null`, or don't generate `GEMINI` for the class. See [Recursive properties](../guides/gemini.md#recursive-properties). |

## Configuration
These don't point at code.

| Message                                                                           | Cause                                          | Fix                                           |
|-----------------------------------------------------------------------------------|------------------------------------------------|-----------------------------------------------|
| `No targets set in 'jsonSchema.targets'. Valid targets are: [gemini, openai]`     | The `jsonSchema.targets` argument is empty.    | List at least one dialect, or remove the argument to generate all of them. |
| `Unknown targets [<target>] in 'jsonSchema.targets'. Valid targets are: [gemini, openai]` | A dialect in the list isn't known.     | Fix the name, or update the library if the dialect is newer. |

## Warnings
| Message                                                                 | Cause                                                       | Fix                    |
|-------------------------------------------------------------------------|-------------------------------------------------------------|------------------------|
| `Unknown option 'jsonSchema.<key>' is ignored. Valid options are: [jsonSchema.targets]` | A `jsonSchema.*` KSP argument isn't known, likely a typo. | Fix or remove the argument. |

## At runtime
The generated object throws an `IllegalArgumentException` from `get(target)` when the dialect isn't generated for the
class. See [Dialects that aren't generated](../guides/runtime-targets.md#dialects-that-arent-generated).
