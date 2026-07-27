package no.nav.tiltakspenger.tiltak.services

/**
 * Oppslaget av tiltakshistorikk feilet.
 * Feilen er allerede logget i [TiltakshistorikkService] med full HTTP-kontekst, så routene trenger bare å oversette den til `500` — det samme konsumentene fikk før migreringen, da klientene kastet.
 * Typen bærer derfor bevisst ingen detaljer: ingen konsument skiller på årsak, og detaljene hører hjemme i logglinja, ikke i responsen.
 */
data object KunneIkkeHenteTiltakshistorikk
