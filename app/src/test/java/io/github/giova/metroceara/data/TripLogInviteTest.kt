package io.github.giova.metroceara.data

import org.junit.Assert.assertEquals
import org.junit.Test

/** O convite para mandar o registro: no máximo um por semana; dois ignorados, para. */
class TripLogInviteTest {

    private val day = 24 * 60 * 60 * 1000L

    @Test
    fun `primeira viagem com problema convida`() {
        val (_, invite) = TripLog.InviteState().onProblem(10 * day)
        assertEquals(true, invite)
    }

    @Test
    fun `outra na mesma semana nao convida`() {
        val (s1, _) = TripLog.InviteState().onProblem(10 * day)
        val (_, invite) = s1.onProblem(13 * day)
        assertEquals(false, invite)
    }

    @Test
    fun `dois convites ignorados param de perguntar`() {
        var state = TripLog.InviteState()
        state = state.onProblem(10 * day).first // 1º convite
        state = state.onProblem(20 * day).first // 2º convite (o 1º ficou sem resposta)
        val (_, invite) = state.onProblem(30 * day) // o 2º também ficou sem resposta
        assertEquals(false, invite)
        assertEquals(2, state.onProblem(30 * day).first.ignored)
    }

    @Test
    fun `mandar o registro volta a convidar`() {
        var state = TripLog.InviteState()
        state = state.onProblem(10 * day).first
        state = state.onProblem(20 * day).first
        state = state.onSent()
        val (_, invite) = state.onProblem(30 * day)
        assertEquals(true, invite)
    }
}
