# Types

How Kotlin types and properties are described. These rules are the same in every dialect, except where
[a dialect rejects a type](#types-a-dialect-rejects).

## Types
| Kotlin                                                            | Schema                                        |
|-------------------------------------------------------------------|-----------------------------------------------|
| `Int`, `Long`, `Short`, `Byte`                                    | `integer`                                     |
| `UInt`, `ULong`, `UShort`, `UByte`                                | `integer` with `minimum` 0                    |
| `Float`, `Double`                                                 | `number`                                      |
| `Boolean`                                                         | `boolean`                                     |
| `String`, `Char`                                                  | `string`                                      |
| Enum classes                                                      | `string` with `enum` set to the entry names   |
| `Array`, `List`, `Set`, `Collection`, `Iterable` (mutable or not) | `array`, with `items` from the element type   |
| `IntArray`, `BooleanArray` and the other primitive arrays         | `array` of the matching primitive             |
| `Map<String, V>` and `MutableMap<String, V>`                      | `object` with `additionalProperties` from `V` |
| Value classes                                                     | The type they wrap                            |
| Type aliases                                                      | The type they stand for                       |
| Other classes and objects                                         | `object` with their `properties`              |

- **Nullable types** allow `null`: `"type": ["string", "null"]`. A nullable enum also lists `null` among its values
  (`"enum": ["METRIC", "IMPERIAL", null]`), since `enum` restricts the value on top of `type`.
- **Value classes** (`@JvmInline value class UserId(val value: String)`) are described by the type they wrap, as
  kotlinx.serialization encodes them as the bare value.
- **Type aliases** are resolved to the type they stand for, keeping their nullability.
- **Map keys** must be `String`.

### Types a dialect rejects
| Type             | `GEMINI`  | `OPENAI`                                                              |
|------------------|-----------|-----------------------------------------------------------------------|
| `Map<String, V>` | Supported | Build error. See [Replacing a map](../guides/openai.md#replacing-a-map) |
| A required, nullable property of its own class, e.g. `val parent: Category?` in `Category` | Build error. See [Recursive properties](../guides/gemini.md#recursive-properties) | Supported |

### Types that aren't supported
These fail the build in every dialect; see [Errors](errors.md#types).

- `Any`, since it could be anything.
- Interfaces, sealed classes and abstract classes, until `anyOf` is supported (see the [roadmap](../roadmap.md)).
- Java classes aren't mapped yet, so they're described by their private fields, or fail.

## Properties
- Only properties with a backing field are included. Computed and delegated properties aren't data, so they're left
  out. Inherited properties are included.
- Properties marked with `@SchemaExclude` are left out.
- Property names are the Kotlin names. Names from serialization annotations, like `@SerialName`, aren't read yet; see
  the [roadmap](../roadmap.md).
- Properties are written in declaration order today. The order isn't a documented guarantee yet; see the
  [roadmap](../roadmap.md).

### Required properties
Which properties are required depends on the dialect; see
[Required and nullable properties](../concepts.md#required-and-nullable-properties). In the dialects that have
optional properties:

- Primary constructor parameters without a default are required. They're matched to properties by name in the class
  being described, so an inherited property is required when the subclass's constructor takes it without a default.
- Properties with a default, or declared in the class body, are optional, since they always have an initial value.
- `@SchemaRequired` makes a property required, even with a default.

For classes from compiled libraries, see the [known issues](../roadmap.md#known-issues).

## Generics
- Type parameters take the type arguments of each use, so `Box<String>` and `Box<Int>` get different schemas.
- `@SchemaRoot` classes can't have type parameters.

## Recursion and shared classes
- A root that contains itself is referenced with `"$ref": "#"`.
- A nested class that contains itself, or is used in more than one place, is defined once in `$defs` and referenced
  with `"$ref": "#/$defs/<Name>"`.
- A nullable reference becomes `"anyOf": [{"$ref": ...}, {"type": "null"}]`.
- Objects can nest at most 32 deep, which catches generic types that grow without repeating, e.g. a `Box<T>` holding
  a `Box<List<T>>`.

For example:

```kotlin
data class Employee(
    val name: String,
    val reports: List<Employee>,
    val manager: Employee? = null
)

@SchemaRoot
data class Organization(val head: Employee)
```

gives, in the `GEMINI` dialect:

```json
{
  "type": "object",
  "properties": {
    "head": {"$ref": "#/$defs/Employee"}
  },
  "required": ["head"],
  "$defs": {
    "Employee": {
      "type": "object",
      "properties": {
        "name": {"type": "string"},
        "reports": {
          "type": "array",
          "items": {"$ref": "#/$defs/Employee"}
        },
        "manager": {
          "anyOf": [{"$ref": "#/$defs/Employee"}, {"type": "null"}]
        }
      },
      "required": ["name", "reports"]
    }
  }
}
```
