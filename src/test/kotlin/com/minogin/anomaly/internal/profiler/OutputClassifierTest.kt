package com.minogin.anomaly.internal.profiler

import com.minogin.anomaly.internal.profiler.model.JsonSchema
import com.minogin.anomaly.internal.profiler.model.OutputForm
import org.junit.jupiter.api.Test
import kotlin.test.*

class OutputClassifierTest {

    private val classifier = OutputClassifier()

    @Test
    fun `classifies empty output`() {
        assertForm("", OutputForm.Type.EMPTY, quoted = false)
        assertForm("   ", OutputForm.Type.EMPTY, quoted = false)
        assertForm("\n\t  \n", OutputForm.Type.EMPTY, quoted = false)
    }

    @Test
    fun `classifies json object`() {
        assertForm(
            """{"riskLevel":"HIGH"}""",
            OutputForm.Type.JSON_OBJECT,
            quoted = false
        )

        assertForm(
            """
            {
              "riskLevel": "HIGH",
              "reason": "Sanctions match"
            }
            """.trimIndent(),
            OutputForm.Type.JSON_OBJECT,
            quoted = false
        )
    }

    @Test
    fun `classifies json array`() {
        assertForm(
            """["HIGH", "LOW"]""",
            OutputForm.Type.JSON_ARRAY,
            quoted = false
        )

        assertForm(
            """
            [
              {"riskLevel": "LOW"},
              {"riskLevel": "HIGH"}
            ]
            """.trimIndent(),
            OutputForm.Type.JSON_ARRAY,
            quoted = false
        )
    }

    @Test
    fun `classifies json integer`() {
        assertForm("42", OutputForm.Type.INTEGER, quoted = false)
        assertForm("-42", OutputForm.Type.INTEGER, quoted = false)
        assertForm("0", OutputForm.Type.INTEGER, quoted = false)
    }

    @Test
    fun `classifies json decimal`() {
        assertForm("42.5", OutputForm.Type.DECIMAL, quoted = false)
        assertForm("-42.5", OutputForm.Type.DECIMAL, quoted = false)
        assertForm("0.001", OutputForm.Type.DECIMAL, quoted = false)
        assertForm("1e10", OutputForm.Type.DECIMAL, quoted = false)
        assertForm("1.5e10", OutputForm.Type.DECIMAL, quoted = false)
        assertForm("-3E-4", OutputForm.Type.DECIMAL, quoted = false)
    }

    @Test
    fun `classifies json string as quoted string`() {
        assertForm(
            """"HIGH"""",
            OutputForm.Type.STRING,
            quoted = true
        )

        assertForm(
            """"REQUEST_MORE_INFORMATION"""",
            OutputForm.Type.STRING,
            quoted = true
        )
    }

    @Test
    fun `classifies quoted json object`() {
        assertForm(
            """"{\"riskLevel\":\"HIGH\"}"""",
            OutputForm.Type.JSON_OBJECT,
            quoted = true
        )

        assertForm(
            """"{\"riskLevel\":\"LOW\",\"reason\":\"Supplier is active.\"}"""",
            OutputForm.Type.JSON_OBJECT,
            quoted = true
        )
    }

    @Test
    fun `classifies quoted json array`() {
        assertForm(
            """"[\"HIGH\",\"LOW\"]"""",
            OutputForm.Type.JSON_ARRAY,
            quoted = true
        )

        assertForm(
            """"[{\"riskLevel\":\"LOW\"},{\"riskLevel\":\"HIGH\"}]"""",
            OutputForm.Type.JSON_ARRAY,
            quoted = true
        )
    }

    @Test
    fun `classifies quoted integer`() {
        assertForm(
            """"42"""",
            OutputForm.Type.INTEGER,
            quoted = true
        )

        assertForm(
            """"-42"""",
            OutputForm.Type.INTEGER,
            quoted = true
        )
    }

    @Test
    fun `classifies quoted decimal`() {
        assertForm(
            """"42.5"""",
            OutputForm.Type.DECIMAL,
            quoted = true
        )

        assertForm(
            """"-0.75"""",
            OutputForm.Type.DECIMAL,
            quoted = true
        )
    }

    @Test
    fun `classifies plain scalar strings`() {
        assertForm(
            "HIGH",
            OutputForm.Type.STRING,
            quoted = false
        )

        assertForm(
            "REQUEST_MORE_INFORMATION",
            OutputForm.Type.STRING,
            quoted = false
        )

        assertForm(
            "The supplier requires more information before approval.",
            OutputForm.Type.STRING,
            quoted = false
        )
    }

    @Test
    fun `classifies multiline plain text as string`() {
        assertForm(
            """
            Supplier should be reviewed.
            Ownership is unclear.
            """.trimIndent(),
            OutputForm.Type.STRING,
            quoted = false
        )
    }

    @Test
    fun `classifies markdown code fence`() {
        assertForm(
            """
            ```json
            {"riskLevel":"HIGH"}
            ```
            """.trimIndent(),
            OutputForm.Type.MARKDOWN,
            quoted = false
        )
    }

    @Test
    fun `classifies markdown headings`() {
        assertForm(
            """
            # Risk report
            
            Supplier should be reviewed.
            """.trimIndent(),
            OutputForm.Type.MARKDOWN,
            quoted = false
        )

        assertForm(
            """
            ## Risk report
            
            Supplier should be reviewed.
            """.trimIndent(),
            OutputForm.Type.MARKDOWN,
            quoted = false
        )

        assertForm(
            """
            ### Risk report
            
            Supplier should be reviewed.
            """.trimIndent(),
            OutputForm.Type.MARKDOWN,
            quoted = false
        )
    }

    @Test
    fun `classifies markdown bullet list`() {
        assertForm(
            """
            - ownership unknown
            - no sanctions match
            """.trimIndent(),
            OutputForm.Type.MARKDOWN,
            quoted = false
        )

        assertForm(
            """
            * ownership unknown
            * no sanctions match
            """.trimIndent(),
            OutputForm.Type.MARKDOWN,
            quoted = false
        )
    }

    @Test
    fun `classifies markdown bold`() {
        assertForm("**Other**", OutputForm.Type.MARKDOWN, quoted = false)
        assertForm("**High Risk**", OutputForm.Type.MARKDOWN, quoted = false)
        assertForm("__Other__", OutputForm.Type.MARKDOWN, quoted = false)
    }

    @Test
    fun `classifies markdown numbered list`() {
        assertForm("1. First\n2. Second", OutputForm.Type.MARKDOWN, quoted = false)
    }

    @Test
    fun `classifies markdown blockquote`() {
        assertForm("> Some quoted text", OutputForm.Type.MARKDOWN, quoted = false)
    }

    @Test
    fun `classifies markdown inline code`() {
        assertForm("Use `val` instead of `var`", OutputForm.Type.MARKDOWN, quoted = false)
    }

    @Test
    fun `classifies markdown strikethrough`() {
        assertForm("~~deprecated~~", OutputForm.Type.MARKDOWN, quoted = false)
    }

    @Test
    fun `classifies markdown image`() {
        assertForm("![logo](https://example.com/logo.png)", OutputForm.Type.MARKDOWN, quoted = false)
    }

    @Test
    fun `classifies markdown table`() {
        assertForm("| Name | Value |\n|------|-------|\n| foo  | bar   |", OutputForm.Type.MARKDOWN, quoted = false)
    }

    @Test
    fun `classifies markdown link`() {
        assertForm(
            "See [supplier profile](https://example.com/supplier/acme).",
            OutputForm.Type.MARKDOWN,
            quoted = false
        )
    }

    @Test
    fun `classifies quoted markdown`() {
        assertForm(
            """"# Risk report"""",
            OutputForm.Type.MARKDOWN,
            quoted = true
        )

        assertForm(
            """"- ownership unknown\n- no sanctions match"""",
            OutputForm.Type.MARKDOWN,
            quoted = true
        )
    }

    @Test
    fun `classifies html`() {
        assertForm(
            "<p>Supplier should be reviewed.</p>",
            OutputForm.Type.HTML,
            quoted = false
        )

        assertForm(
            """
            <div>
              <span>Supplier should be reviewed.</span>
            </div>
            """.trimIndent(),
            OutputForm.Type.HTML,
            quoted = false
        )
    }

    @Test
    fun `classifies quoted html`() {
        assertForm(
            """"<p>Supplier should be reviewed.</p>"""",
            OutputForm.Type.HTML,
            quoted = true
        )
    }

    @Test
    fun `malformed json object is classified as string`() {
        assertForm(
            """{"riskLevel":"HIGH"""",
            OutputForm.Type.STRING,
            quoted = false
        )
    }

    @Test
    fun `malformed quoted json object is classified as string`() {
        assertForm(
            """"{\"riskLevel\":\"HIGH\""""",
            OutputForm.Type.STRING,
            quoted = true
        )
    }

    @Test
    fun `json object takes precedence over markdown-like string inside json`() {
        assertForm(
            """{"text":"# Risk report"}""",
            OutputForm.Type.JSON_OBJECT,
            quoted = false
        )
    }

    @Test
    fun `quoted json object takes precedence over markdown-like content inside json`() {
        assertForm(
            """"{\"text\":\"# Risk report\"}"""",
            OutputForm.Type.JSON_OBJECT,
            quoted = true
        )
    }

    @Test
    fun `html takes precedence over plain string`() {
        assertForm(
            "<strong>APPROVE</strong>",
            OutputForm.Type.HTML,
            quoted = false
        )
    }

    @Test
    fun `json object schema captures field names and value types`() {
        assertSchema(
            """{"riskLevel":"HIGH","score":42}""",
            JsonSchema.ObjectSchema(
                mapOf(
                    "riskLevel" to JsonSchema.Primitive.STRING,
                    "score" to JsonSchema.Primitive.INTEGER
                )
            )
        )
    }

    @Test
    fun `json object schema captures nested objects`() {
        assertSchema(
            """{"result":{"status":"ok","count":3}}""",
            JsonSchema.ObjectSchema(
                mapOf(
                    "result" to JsonSchema.ObjectSchema(
                        mapOf(
                            "status" to JsonSchema.Primitive.STRING,
                            "count" to JsonSchema.Primitive.INTEGER
                        )
                    )
                )
            )
        )
    }

    @Test
    fun `json array schema captures homogeneous element type`() {
        assertSchema(
            """[1, 2, 3]""",
            JsonSchema.ArraySchema(setOf(JsonSchema.Primitive.INTEGER))
        )
    }

    @Test
    fun `json array schema captures object element shape`() {
        assertSchema(
            """[{"id":1,"name":"foo"},{"id":2,"name":"bar"}]""",
            JsonSchema.ArraySchema(
                setOf(
                    JsonSchema.ObjectSchema(
                        mapOf(
                            "id" to JsonSchema.Primitive.INTEGER,
                            "name" to JsonSchema.Primitive.STRING
                        )
                    )
                )
            )
        )
    }

    @Test
    fun `json array schema captures mixed element types`() {
        assertSchema(
            """[1, "foo"]""",
            JsonSchema.ArraySchema(setOf(JsonSchema.Primitive.INTEGER, JsonSchema.Primitive.STRING))
        )
    }

    @Test
    fun `empty json array schema has no element schemas`() {
        assertSchema(
            """[]""",
            JsonSchema.ArraySchema(emptySet())
        )
    }

    @Test
    fun `quoted json object carries schema`() {
        assertSchema(
            """"{\"riskLevel\":\"HIGH\",\"score\":42}"""",
            JsonSchema.ObjectSchema(
                mapOf(
                    "riskLevel" to JsonSchema.Primitive.STRING,
                    "score" to JsonSchema.Primitive.INTEGER
                )
            )
        )
    }

    @Test
    fun `json object schema captures boolean and null fields`() {
        assertSchema(
            """{"active":true,"reason":null}""",
            JsonSchema.ObjectSchema(
                mapOf(
                    "active" to JsonSchema.Primitive.BOOLEAN,
                    "reason" to JsonSchema.Primitive.NULL
                )
            )
        )
    }

    private fun assertForm(
        output: String,
        expectedType: OutputForm.Type,
        quoted: Boolean
    ) {
        val form = classifier.classify(output)
        assertEquals(expectedType, form.type)
        assertEquals(quoted, form.quoted)
    }

    private fun assertSchema(output: String, expectedSchema: JsonSchema) {
        val form = classifier.classify(output)
        assertEquals(expectedSchema, form.schema)
    }
}