package com.github.mihanizzm.ultistats.dto.response

import com.github.mihanizzm.ultistats.model.MatchParticipant
import com.github.mihanizzm.ultistats.model.MatchParticipantKind
import java.util.UUID

data class MatchParticipantResponse(
    val participantId: UUID,
    val kind: MatchParticipantKind,
    val playerId: UUID?,
    val unknownSlot: Int?,
    val number: Int?,
) {
    companion object {
        fun from(participant: MatchParticipant) = MatchParticipantResponse(
            participantId = participant.participantId,
            kind = participant.kind,
            playerId = participant.playerId,
            unknownSlot = participant.unknownSlot,
            number = participant.number,
        )
    }
}
