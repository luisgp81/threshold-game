package com.medialert.utils

/**
 * All Gemini Vision API prompts centralized here.
 * Adjust prompts here without touching business logic.
 */
object GeminiPrompts {

    /**
     * Case 1: Medical report / prescription scan.
     * Expects a JSON response with a list of medications.
     */
    val SCAN_PRESCRIPTION = """
        Analiza esta imagen de un informe médico o receta. Extrae TODOS los medicamentos mencionados.
        Responde ÚNICAMENTE con este JSON, sin texto adicional:
        {
          "medicamentos": [
            {
              "nombre": "string",
              "principioActivo": "string o null",
              "dosis": "string",
              "formaFarmaceutica": "string",
              "frecuencia": "string",
              "horariosSugeridos": ["HH:mm"],
              "duracionDias": "number o null",
              "instruccionesEspeciales": "string o null",
              "confianza": "number entre 0 y 1"
            }
          ]
        }
    """.trimIndent()

    /**
     * Case 2: Medication box verification.
     * Extracts commercial name, active ingredient, concentration, etc.
     */
    val SCAN_MEDICATION_BOX = """
        Analiza esta imagen de una caja de medicamento.
        Responde ÚNICAMENTE con este JSON, sin texto adicional:
        {
          "nombreComercial": "string",
          "principioActivo": "string",
          "concentracion": "string",
          "formaFarmaceutica": "string",
          "unidadesCaja": "number o null",
          "laboratorio": "string o null",
          "confianza": "number entre 0 y 1"
        }
    """.trimIndent()

    /**
     * Case 3: Dose photo registration.
     * [MEDICATION_NAME] and [DOSE] are replaced at runtime.
     */
    const val REGISTER_DOSE_TEMPLATE = """
        El usuario va a tomar estos medicamentos. Analiza la imagen y confirma qué pastillas,
        cápsulas o medicamentos son visibles. Medicamento esperado: [MEDICATION_NAME] [DOSE].
        Responde ÚNICAMENTE con este JSON, sin texto adicional:
        {
          "medicamentosDetectados": ["array de strings"],
          "coincideConEsperado": "boolean",
          "observaciones": "string o null",
          "confianza": "number entre 0 y 1"
        }
    """

    fun buildRegisterDosePrompt(medicationName: String, dose: String): String {
        return REGISTER_DOSE_TEMPLATE
            .replace("[MEDICATION_NAME]", medicationName)
            .replace("[DOSE]", dose)
            .trimIndent()
    }
}
