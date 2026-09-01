package com.github.mihanizzm.ultistats.dto.response

import com.github.mihanizzm.ultistats.model.MatchParticipant
import com.github.mihanizzm.ultistats.model.MatchParticipantKind
import java.util.UUID

data class MatchParticipantResponse(
    val participantId: UUID,
    val kind: MatchParticipantKind,
    val unknownSlot: Int?,
    val firstName: String?,
    val lastName: String?,
    val displayName: String,
    val number: Int?,
) {
    companion object {
        fun from(participant: MatchParticipant) = MatchParticipantResponse(
            participantId = participant.participantId,
            kind = participant.kind,
            unknownSlot = participant.unknownSlot,
            firstName = participant.firstName,
            lastName = participant.lastName,
            displayName = participant.displayName,
            number = participant.number,
        )
    }
}
