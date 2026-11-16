# Annotations

All annotations are in the `com.vectornorthsystems.jsonschema` package, in the `json-schema-annotations` artifact.

## On a class
### `@SchemaRoot`
Marks the class for schema generation. The processor generates an object with its schemas; see
[The generated object](targets.md#the-generated-object).

| Parameter | Type                  | Default               | What it does                                                             |
|-----------|-----------------------|-----------------------|--------------------------------------------------------------------------|
| `name`    | `String`              | `{ClassName}Schema`   | The name of the generated object. It must be a valid Kotlin identifier, and unique in the package. |
| `targets` | `Array<SchemaTarget>` | The module's dialects | The dialects to generate for this class, overriding the module's. See [Choosing the dialects](targets.md#choosing-the-dialects). |

```kotlin
// Generation setup
@SchemaRoot(name = "StarCraft", targets = [SchemaTarget.GEMINI])
data class Star(val type: String)

// Accessing the schema
val schema = StarCraft.GEMINI
```

- The class must be a concrete class or an object, without type parameters.
- The generated object is public, or `internal` when the class is internal or private.
- Two nested classes with the same name in a package would get the same object, so give one of them a `name`.

## On a property
| Annotation                           | Applies to           | Writes                                    | `GEMINI` | `OPENAI`  |
|--------------------------------------|----------------------|-------------------------------------------|----------|-----------|
| `@SchemaTitle(text)`                 | Any property         | `title`                                   | Yes      | Yes       |
| `@SchemaDescription(text)`           | Any property         | `description`                             | Yes      | Yes       |
| `@SchemaRequired`                    | Any property         | Adds the property to `required`           | Yes      | No effect |
| `@SchemaExclude`                     | Any property         | Leaves the property out of the schema     | Yes      | Yes       |
| `@SchemaFormat(format)`              | `String` (not enums) | `format`                                  | Yes      | Yes       |
| `@SchemaMinimum(value)`              | Integers and numbers | `minimum`, inclusive                      | Yes      | Yes       |
| `@SchemaMaximum(value)`              | Integers and numbers | `maximum`, inclusive                      | Yes      | Yes       |
| `@SchemaMinItems(value)`             | Collections          | `minItems`                                | Yes      | Yes       |
| `@SchemaMaxItems(value)`             | Collections          | `maxItems`                                | Yes      | Yes       |

An annotation on a property it doesn't apply to fails the build; see [Errors](errors.md#annotations).

### `@SchemaTitle` and `@SchemaDescription`
A short name for the property, and a description of it. The description is the main way to tell the AI model what to put
in a property.

### `@SchemaRequired`
Marks a property the model must include, even though it has a default. Use it when the default is only a convenience
for your own code. It has no effect in the dialects where every property is already required. See
[Required and nullable properties](../concepts.md#required-and-nullable-properties).

### `@SchemaExclude`
Leaves a property out of the schema, so the model doesn't produce it. The property needs a default, or the answer
may not decode, depending on the serialization library used.

### `@SchemaFormat`
The syntax of a `String` property:

| `SchemaFormat.Format` | `format`    |
|-----------------------|-------------|
| `DATE_TIME`           | `date-time` |
| `DATE`                | `date`      |
| `TIME`                | `time`      |

### `@SchemaMinimum` and `@SchemaMaximum`
The inclusive bounds of a number. They take a `Double`, which must be finite, and the minimum can't be greater than
the maximum. On an unsigned type, `@SchemaMinimum` replaces the `minimum` of 0.

### `@SchemaMinItems` and `@SchemaMaxItems`
The bounds of the number of items in a collection. They can't be negative, and the minimum can't be greater than the
maximum.

## Example: `GEMINI` and `OPENAI`
```kotlin
@SchemaRoot
data class Event(
    @SchemaTitle("Name")
    @SchemaDescription("The name of the event, as advertised")
    val name: String,

    val notes: String? = null,

    @SchemaFormat(SchemaFormat.Format.DATE_TIME)
    val startsAt: String,

    @SchemaMinimum(1.0)
    @SchemaMaximum(4.5)
    val rating: Double,

    @SchemaMinimum(0.0)
    val attendees: Int = 0,

    @SchemaMinItems(1)
    @SchemaMaxItems(20)
    val speakers: List<String>
)
```

`EventSchema.GEMINI` (`-`) and `EventSchema.OPENAI` (`+`) have the same properties. `OPENAI` adds
`"additionalProperties": false` and puts every property in `required`:

```diff
 {
   "type": "object",
   "properties": {
     "name": {
       "title": "Name",
       "description": "The name of the event, as advertised",
       "type": "string"
     },
     "notes": {"type": ["string", "null"]},
     "startsAt": {"type": "string", "format": "date-time"},
     "rating": {"type": "number", "minimum": 1, "maximum": 4.5},
     "attendees": {"type": "integer", "minimum": 0},
     "speakers": {
       "type": "array",
       "items": {"type": "string"},
       "minItems": 1,
       "maxItems": 20
     }
   },
-  "required": ["name", "startsAt", "rating", "speakers"]
+  "additionalProperties": false,
+  "required": ["name", "notes", "startsAt", "rating", "attendees", "speakers"]
 }
```
