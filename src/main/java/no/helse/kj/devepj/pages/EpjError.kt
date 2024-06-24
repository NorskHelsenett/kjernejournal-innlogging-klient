package no.helse.kj.devepj.pages

import io.javalin.http.Context
import java.util.Arrays
import java.util.stream.Collectors

data class EpjError(val context: String, val status: Int, val message: String?, val cause: Throwable? = null)

fun visFeilside(
  ctx: Context,
  kilde: String,
  httpStatus: Int,
  kontekst: String,
  melding: String,
  stackTrace: Array<StackTraceElement>? = emptyArray(),
) {
  ctx.render(
    "/templates/error.ftl", mapOf(
      Pair("kilde", kilde),
      Pair("status", httpStatus),
      Pair("kontekst", kontekst),
      Pair("melding", melding),
      Pair("stacktrace", Arrays.stream(stackTrace)
        .map { obj: StackTraceElement -> obj.toString() }
        .collect(Collectors.joining("\n"))
      )
    )
  )
}

fun visError(
  ctx: Context,
  error: EpjError,
  kilde: String = "Ukjent kilde",
) {
  ctx.render(
    "/templates/error.ftl", mapOf(
      Pair("kilde", kilde),
      Pair("status", error.status),
      Pair("kontekst", error.context),
      Pair("melding", error.message ?: ""),
      Pair("stacktrace", error.cause?.stackTrace
        ?.map { obj: StackTraceElement -> obj.toString() }
        ?.joinToString("\n") ?: ""
      )
    )
  )
}
