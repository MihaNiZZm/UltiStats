package com.github.mihanizzm.ultistats.dto.response

import com.github.mihanizzm.ultistats.model.Team
import java.util.UUID

data class TeamListItemResponse(
    val id: UUID,
    val name: String,
    val city: String?,
    val photoUrl: String?,
) {
    companion object {
        fun from(team: Team): TeamListItemResponse =
            TeamListItemResponse(
                id = team.id,
                name = team.name,
                city = team.city,
                photoUrl = team.photoUrl,
            )
    }
}
