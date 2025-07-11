package com.example

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class Poll(val id: Int, val question: String)

@Serializable
data class PollOption(val id: Int, val text: String, var votesCount: Int = 0, val pollId: Int)

@Serializable
data class PollRequest(val question: String)

@Serializable
data class PollOptionRequest(val text: String, val pollId: Int)

@Serializable
data class PollResult(val id: Int, val question: String, val options: List<PollOption>)

object PollRepository {
    private val polls = mutableListOf<Poll>()
    private val pollOptions = mutableListOf<PollOption>()

    fun getallPolls(): List<Poll> = polls

    fun getPollById(id: Int): Poll? = polls.find { it.id == id }

    fun addPoll(poll: Poll) = polls.add(poll)

    fun updatePoll(poll: Poll) {
        val index = polls.indexOfFirst { it.id == poll.id }
        if (index != -1) {
            polls[index] = poll
        }
    }

    fun deletePoll(id: Int): Boolean {
        pollOptions.removeIf { it.pollId == id }
        return polls.removeIf { it.id == id }
    }
    //-------------------------------พูลออฟชั่น

    fun getOptionsforPoll(pollId: Int): List<PollOption> = pollOptions.filter { it.pollId == pollId }

    fun getOptionById(id: Int): PollOption? = pollOptions.find { it.id == id }

    fun addPollOption(pollOption: PollOption) = pollOptions.add(pollOption)

    fun updatePollOption(pollOption: PollOption) {
        val index = pollOptions.indexOfFirst { it.id == pollOption.id }
        if (index != -1) {
            pollOptions[index] = pollOption
        }
    }

    fun deletePollOption(id: Int) = pollOptions.removeIf { it.id == id }
    //-------------------------------------------โหวด

    fun VoteCount(optionId: Int) : Boolean{
        var option = pollOptions.find { it.id == optionId }
        return if (option != null) {
            option.votesCount++
            true
        }
        else{
            false
        }
    }

    fun getPollResult(pollId: Int): PollResult? {
        val poll = getPollById(pollId) ?: return null
        val options = getOptionsforPoll(pollId)
        return PollResult(poll.id, poll.question, options)
    }

    fun getNextPollId(): Int = polls.maxOfOrNull { it.id }?.plus(1) ?: 1
    fun getNextOptionId(): Int = pollOptions.maxOfOrNull { it.id }?.plus(1) ?: 1
}


fun Application.configureRouting() {
    routing {
        //-----------------------โพล
        get("/polls") {
            val allPollResults = PollRepository.getallPolls().map { poll ->
                PollRepository.getPollResult(poll.id)!! // มั่นใจว่ามี เพราะดึงมาจาก getAllPolls
            }
            call.respond(allPollResults)
        }

        get("/polls/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@get call.respondText(
                "Invalid poll id",
                status = HttpStatusCode.BadRequest
            )
            val pollResult = PollRepository.getPollResult(id) ?: return@get call.respondText(
                "No poll with id $id",
                status = HttpStatusCode.NotFound
            )
            call.respond(pollResult)
        }

        post("/polls") {
            val poll = call.receive<PollRequest>()
            val pollId = PollRepository.getNextPollId()
            val newPoll = Poll(pollId, poll.question)
            PollRepository.addPoll(newPoll)
            call.respond(newPoll)
        }

        put("/polls/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@put call.respondText(
                "Invalid poll id",
                status = HttpStatusCode.BadRequest
            )
            PollRepository.getPollById(id) ?: return@put call.respondText(
                "No poll with id $id",
                status = HttpStatusCode.NotFound
            )

            val pollRequest = call.receive<PollRequest>()
            val updatedPoll = Poll(id, pollRequest.question)
            PollRepository.updatePoll(updatedPoll)
            call.respond(updatedPoll)
        }

        delete("/polls/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
            PollRepository.deletePoll(id)
            call.respondText("Poll $id deleted", status = HttpStatusCode.OK)
        }

        //---------------------------------ออฟชั่น

        get("/polls/{pollId}/options") {
            val pollId = call.parameters["pollId"]?.toIntOrNull() ?: return@get call.respondText(
                "Invalid poll id",
                status = HttpStatusCode.BadRequest
            )
            val options = PollRepository.getOptionsforPoll(pollId)
            call.respond(options)
        }

        post("/options") {
            val pollOptionRequest = call.receive<PollOptionRequest>()
            val pollId = pollOptionRequest.pollId

            PollRepository.getPollById(pollId) ?: return@post call.respondText(
                "Poll with id $pollId not found",
                status = HttpStatusCode.NotFound
            )

            val optionId = PollRepository.getNextOptionId()
            val newOption = PollOption(optionId, pollOptionRequest.text, pollId = pollId)
            PollRepository.addPollOption(newOption)
            call.respond(HttpStatusCode.Created, newOption)
        }

        put("/options/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@put call.respondText(
                "Invalid option id",
                status = HttpStatusCode.BadRequest
            )
            val existingOption = PollRepository.getOptionById(id) ?: return@put call.respondText(
                "No option with id $id",
                status = HttpStatusCode.NotFound
            )
            val optionRequest = call.receive<PollOptionRequest>()
            val updatedOption = PollOption(id, optionRequest.text, existingOption.votesCount, existingOption.pollId)
            PollRepository.updatePollOption(updatedOption)
            call.respond(updatedOption)
        }

        delete("options/{id}") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@delete call.respond(HttpStatusCode.BadRequest)
            PollRepository.deletePollOption(id)
            call.respondText("Option $id deleted", status = HttpStatusCode.OK)
        }

        //-------------------------------------------------------------------โหวด

        post("options/{id}/votes") {
            val id = call.parameters["id"]?.toIntOrNull() ?: return@post call.respondText(
                "Invalid option id",
                status = HttpStatusCode.BadRequest
            )

            if (PollRepository.getOptionById(id) == null) {
                return@post call.respondText(
                    "No option with id $id",
                    status = HttpStatusCode.NotFound
                )
            }

            val isSuccess = PollRepository.VoteCount(id)

            if (isSuccess) {
                val updatedOption = PollRepository.getOptionById(id)
                call.respond(HttpStatusCode.OK, updatedOption!!)
            } else {
                call.respond(HttpStatusCode.InternalServerError, "Failed to cast vote")
            }
        }

    }
}
