package no.nav.tiltakspenger.tiltak.infra.http

/**
 * GraphQL-konvolutten slik tjenestene svarer den.
 * [data] er nullable fordi PDL svarer `200` med `data: null` når spørringen feiler funksjonelt; feilene ligger da i [errors].
 */
data class GraphQLResponse<T>(
    val data: T? = null,
    val errors: List<GraphQLResponseError>? = null,
) {
    data class GraphQLResponseError(
        val message: String?,
        val locations: List<ErrorLocation>?,
        val path: List<String>?,
        val extensions: ErrorExtension?,
    )

    data class ErrorLocation(
        val line: String?,
        val column: String?,
    )

    data class ErrorExtension(
        val code: String?,
        val classification: String?,
    )
}
