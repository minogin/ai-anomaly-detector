// TODO
//package com.minogin.checkpoint
//
//import com.minogin.checkpoint.tracer.Sampler
//import org.junit.jupiter.api.*
//
//class AiWorkflowFramingScenarioTest {
//
//    @Test
//    fun `baseline run before changes`() {
//        val sampler = Sampler("1.0")
//
//        val userRequest = """
//            Investigate supplier ACME Metals TGmbH.
//            Decide whether onboarding can continue.
//        """.trimIndent()
//
//        sampler.checkpoint(
//            step = "workflow-input",
//            input = "user-request",
//            output = userRequest
//        )
//
//        val extractSupplierInput = """
//            Extract supplier identity.
//
//            Request:
//            $userRequest
//        """.trimIndent()
//
//        val extractSupplierOutput = """
//            {
//              "companyName": "ACME Metals GmbH",
//              "countryHint": "AT"
//            }
//        """.trimIndent()
//
//        sampler.checkpoint(
//            step = "llm-extract-supplier",
//            input = extractSupplierInput,
//            output = extractSupplierOutput
//        )
//
//        val supplierLookupCallInput = extractSupplierOutput
//
//        val supplierLookupCallOutput = """
//            {
//              "tool": "supplier_profile_lookup",
//              "arguments": {
//                "companyName": "ACME Metals GmbH",
//                "countryHint": "AT"
//              }
//            }
//        """.trimIndent()
//
//        sampler.checkpoint(
//            step = "tool-call-supplier-profile-lookup",
//            input = supplierLookupCallInput,
//            output = supplierLookupCallOutput
//        )
//
//        val supplierLookupResultInput = supplierLookupCallOutput
//
//        val supplierLookupResultOutput = """
//            {
//              "companyName": "ACME Metals GmbH",
//              "registeredCountry": "AT",
//              "active": true,
//              "ownershipStatus": "UNKNOWN"
//            }
//        """.trimIndent()
//
//        sampler.checkpoint(
//            step = "tool-result-supplier-profile-lookup",
//            input = supplierLookupResultInput,
//            output = supplierLookupResultOutput
//        )
//
//        val sanctionsCallInput = supplierLookupResultOutput
//
//        val sanctionsCallOutput = """
//            {
//              "tool": "sanctions_screening",
//              "arguments": {
//                "companyName": "ACME Metals GmbH",
//                "country": "AT"
//              }
//            }
//        """.trimIndent()
//
//        sampler.checkpoint(
//            step = "tool-call-sanctions-screening",
//            input = sanctionsCallInput,
//            output = sanctionsCallOutput
//        )
//
//        val sanctionsResultInput = sanctionsCallOutput
//
//        val sanctionsResultOutput = """
//            {
//              "matches": [],
//              "status": "CLEAR"
//            }
//        """.trimIndent()
//
//        sampler.checkpoint(
//            step = "tool-result-sanctions-screening",
//            input = sanctionsResultInput,
//            output = sanctionsResultOutput
//        )
//
//        val ownershipDecisionInput = """
//            Supplier profile:
//            $supplierLookupResultOutput
//
//            Sanctions result:
//            $sanctionsResultOutput
//
//            Should we perform additional ownership lookup?
//            Return YES or NO.
//        """.trimIndent()
//
//        val ownershipDecisionOutput = "YES"
//
//        sampler.checkpoint(
//            step = "llm-decide-ownership-lookup",
//            input = ownershipDecisionInput,
//            output = ownershipDecisionOutput
//        )
//
//        val ownershipLookupCallInput = ownershipDecisionOutput
//
//        val ownershipLookupCallOutput = """
//            {
//              "tool": "company_registry_lookup",
//              "arguments": {
//                "companyName": "ACME Metals GmbH",
//                "country": "AT"
//              }
//            }
//        """.trimIndent()
//
//        sampler.checkpoint(
//            step = "tool-call-company-registry-lookup",
//            input = ownershipLookupCallInput,
//            output = ownershipLookupCallOutput
//        )
//
//        val ownershipLookupResultInput = ownershipLookupCallOutput
//
//        val ownershipLookupResultOutput = """
//            {
//              "beneficialOwners": [],
//              "ownershipStatus": "NOT_DISCLOSED"
//            }
//        """.trimIndent()
//
//        sampler.checkpoint(
//            step = "tool-result-company-registry-lookup",
//            input = ownershipLookupResultInput,
//            output = ownershipLookupResultOutput
//        )
//
//        val riskClassificationInput = """
//            Classify supplier risk.
//
//            Supplier profile:
//            $supplierLookupResultOutput
//
//            Sanctions:
//            $sanctionsResultOutput
//
//            Ownership:
//            $ownershipLookupResultOutput
//
//            Return JSON with riskLevel and reason.
//        """.trimIndent()
//
//        val riskClassificationOutput = """
//            {
//              "riskLevel": "MEDIUM",
//              "reason": "No sanctions match, but ownership is not disclosed."
//            }
//        """.trimIndent()
//
//        sampler.checkpoint(
//            step = "llm-classify-risk",
//            input = riskClassificationInput,
//            output = riskClassificationOutput
//        )
//
//        val actionSelectionInput = """
//            Select next action.
//
//            Risk:
//            $riskClassificationOutput
//
//            Return one of:
//            APPROVE
//            REQUEST_MORE_INFORMATION
//            ESCALATE_TO_COMPLIANCE
//        """.trimIndent()
//
//        val actionSelectionOutput = "REQUEST_MORE_INFORMATION"
//
//        sampler.checkpoint(
//            step = "llm-select-action",
//            input = actionSelectionInput,
//            output = actionSelectionOutput
//        )
//
//        val messageInput = """
//            Generate internal review message.
//
//            Supplier:
//            ACME Metals GmbH
//
//            Action:
//            $actionSelectionOutput
//        """.trimIndent()
//
//        val messageOutput = """
//            ACME Metals GmbH requires additional beneficial ownership information before onboarding can continue.
//        """.trimIndent()
//
//        sampler.checkpoint(
//            step = "llm-generate-message",
//            input = messageInput,
//            output = messageOutput
//        )
//
//        sampler.checkpoint(
//            step = "workflow-final-result",
//            input = userRequest,
//            output = """
//                {
//                  "supplier": "ACME Metals GmbH",
//                  "riskLevel": "MEDIUM",
//                  "action": "REQUEST_MORE_INFORMATION"
//                }
//            """.trimIndent()
//        )
//
//        sampler.flush()
//    }
//
//    @Test
//    fun `changed run after changes`() {
//        val sampler = Sampler("2.0")
//
//        val userRequest = """
//            Investigate supplier ACME Metals GmbH.
//            Decide whether onboarding can continue.
//        """.trimIndent()
//
//        sampler.checkpoint(
//            step = "workflow-input",
//            input = "user-request",
//            output = userRequest
//        )
//
//        val extractSupplierInput = """
//            Extract supplier identity.
//
//            Request:
//            $userRequest
//        """.trimIndent()
//
//        val extractSupplierOutput = """
//            {
//              "companyName": "ACME Metals",
//              "countryHint": "AT"
//            }
//        """.trimIndent()
//
//        sampler.checkpoint(
//            step = "llm-extract-supplier",
//            input = extractSupplierInput,
//            output = extractSupplierOutput
//        )
//
//        val supplierLookupCallInput = extractSupplierOutput
//
//        val supplierLookupCallOutput = """
//            {
//              "tool": "supplier_profile_lookup",
//              "arguments": {
//                "companyName": "ACME Metals",
//                "countryHint": "AT"
//              }
//            }
//        """.trimIndent()
//
//        sampler.checkpoint(
//            step = "tool-call-supplier-profile-lookup",
//            input = supplierLookupCallInput,
//            output = supplierLookupCallOutput
//        )
//
//        val supplierLookupResultInput = supplierLookupCallOutput
//
//        val supplierLookupResultOutput = """
//            {
//              "companyName": "ACME Metals GmbH",
//              "registeredCountry": "AT",
//              "active": true,
//              "ownershipStatus": "UNKNOWN"
//            }
//        """.trimIndent()
//
//        sampler.checkpoint(
//            step = "tool-result-supplier-profile-lookup",
//            input = supplierLookupResultInput,
//            output = supplierLookupResultOutput
//        )
//
//        val sanctionsCallInput = supplierLookupResultOutput
//
//        val sanctionsCallOutput = """
//            {
//              "tool": "sanctions_screening",
//              "arguments": {
//                "companyName": "ACME Metals",
//                "country": "AT"
//              }
//            }
//        """.trimIndent()
//
//        sampler.checkpoint(
//            step = "tool-call-sanctions-screening",
//            input = sanctionsCallInput,
//            output = sanctionsCallOutput
//        )
//
//        val sanctionsResultInput = sanctionsCallOutput
//
//        val sanctionsResultOutput = """
//            {
//              "matches": [],
//              "status": "CLEAR"
//            }
//        """.trimIndent()
//
//        sampler.checkpoint(
//            step = "tool-result-sanctions-screening",
//            input = sanctionsResultInput,
//            output = sanctionsResultOutput
//        )
//
//        val ownershipDecisionInput = """
//            Supplier profile:
//            $supplierLookupResultOutput
//
//            Sanctions result:
//            $sanctionsResultOutput
//
//            Should we perform additional ownership lookup?
//            Return YES or NO.
//        """.trimIndent()
//
//        val ownershipDecisionOutput = "\"NO\""
//
//        sampler.checkpoint(
//            step = "llm-decide-ownership-lookup",
//            input = ownershipDecisionInput,
//            output = ownershipDecisionOutput
//        )
//
//        val riskClassificationInput = """
//            Classify supplier risk.
//
//            Supplier profile:
//            $supplierLookupResultOutput
//
//            Sanctions:
//            $sanctionsResultOutput
//
//            Ownership:
//            NOT_CHECKED
//
//            Return JSON with riskLevel and reason.
//        """.trimIndent()
//
//        val riskClassificationOutput = """
//            "{\"riskLevel\":\"LOW\",\"reason\":\"Supplier is active, registered in Austria, and no sanctions match was found.\"}"
//        """.trimIndent()
//
//        sampler.checkpoint(
//            step = "llm-classify-risk",
//            input = riskClassificationInput,
//            output = riskClassificationOutput
//        )
//
//        val actionSelectionInput = """
//            Select next action.
//
//            Risk:
//            $riskClassificationOutput
//
//            Return one of:
//            APPROVE
//            REQUEST_MORE_INFORMATION
//            ESCALATE_TO_COMPLIANCE
//        """.trimIndent()
//
//        val actionSelectionOutput = """
//            The supplier can be approved because no sanctions match was found.
//        """.trimIndent()
//
//        sampler.checkpoint(
//            step = "llm-select-action",
//            input = actionSelectionInput,
//            output = actionSelectionOutput
//        )
//
//        val messageInput = """
//            Generate internal review message.
//
//            Supplier:
//            ACME Metals GmbH
//
//            Action:
//            $actionSelectionOutput
//        """.trimIndent()
//
//        val messageOutput = """
//            ACME Metals GmbH can be approved. The supplier is active, registered in Austria, and no sanctions match was found.
//        """.trimIndent()
//
//        sampler.checkpoint(
//            step = "llm-generate-message",
//            input = messageInput,
//            output = messageOutput
//        )
//
//        sampler.checkpoint(
//            step = "workflow-final-result",
//            input = userRequest,
//            output = """
//                {
//                  "supplier": "ACME Metals GmbH",
//                  "riskLevel": "LOW",
//                  "action": "APPROVE"
//                }
//            """.trimIndent()
//        )
//
//        sampler.flush()
//    }
//}