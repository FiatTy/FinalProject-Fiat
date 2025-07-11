package com.example

import kotlin.test.*

class PollRepositoryTest {

    @BeforeTest
    fun clearData() {
        PollRepository.deletePoll(1)
        PollRepository.deletePoll(2)
        PollRepository.deletePollOption(1)
        PollRepository.deletePollOption(2)
    }

    @Test
    fun testAddAndGetPoll() {
        // Arrange: ตั้งค่า input และผลลัพธ์ที่คาดหวัง
        val pollId = PollRepository.getNextPollId()
        val poll = Poll(pollId, "ชอบสัตว์อะไรที่สุด?")
        PollRepository.addPoll(poll)

        // Act: เรียกใช้ฟังก์ชันที่ต้องการทดสอบ
        val result = PollRepository.getPollById(pollId)

        // Assert: ตรวจสอบว่าผลลัพธ์ที่ได้ตรงกับที่คาดหวัง
        assertNotNull(result)
        assertEquals("ชอบสัตว์อะไรที่สุด?", result?.question)
    }

    @Test
    fun testAddOptionAndGetByPollId() {
        // Arrange: ตั้งค่า input และผลลัพธ์ที่คาดหวัง
        val pollId = PollRepository.getNextPollId()
        PollRepository.addPoll(Poll(pollId, "อาหารจานโปรด"))
        val optionId = PollRepository.getNextOptionId()
        val option = PollOption(optionId, "กะเพราหมูสับ", pollId = pollId)

        // Act: เรียกใช้ฟังก์ชันที่ต้องการทดสอบ
        PollRepository.addPollOption(option)
        val options = PollRepository.getOptionsforPoll(pollId)

        // Assert: ตรวจสอบว่าผลลัพธ์ที่ได้ตรงกับที่คาดหวัง
        assertEquals(1, options.size)
        assertEquals("กะเพราหมูสับ", options[0].text)
    }

    @Test
    fun testVoteIncrease() {
        // Arrange: ตั้งค่า input และผลลัพธ์ที่คาดหวัง
        val pollId = PollRepository.getNextPollId()
        PollRepository.addPoll(Poll(pollId, "โหวตทดสอบ"))
        val optionId = PollRepository.getNextOptionId()
        PollRepository.addPollOption(PollOption(optionId, "เลือกอันนี้", pollId = pollId))
        val before = PollRepository.getOptionById(optionId)?.votesCount ?: 0

        // Act: เรียกใช้ฟังก์ชันที่ต้องการทดสอบ
        val voted = PollRepository.VoteCount(optionId)
        val after = PollRepository.getOptionById(optionId)?.votesCount ?: 0

        // Assert: ตรวจสอบว่าผลลัพธ์ที่ได้ตรงกับที่คาดหวัง
        assertTrue(voted)
        assertEquals(before + 1, after)
    }

    @Test
    fun testPollResultHasOptions() {
        // Arrange: ตั้งค่า input และผลลัพธ์ที่คาดหวัง
        val pollId = PollRepository.getNextPollId()
        PollRepository.addPoll(Poll(pollId, "เลือกสิ่งที่คุณชอบ"))
        PollRepository.addPollOption(PollOption(PollRepository.getNextOptionId(), "A", votesCount = 0, pollId = pollId))
        PollRepository.addPollOption(PollOption(PollRepository.getNextOptionId(), "B", votesCount = 0, pollId = pollId))

        // Act: เรียกใช้ฟังก์ชันที่ต้องการทดสอบ
        val result = PollRepository.getPollResult(pollId)

        // Assert: ตรวจสอบว่าผลลัพธ์ที่ได้ตรงกับที่คาดหวัง
        assertNotNull(result)
        assertEquals("เลือกสิ่งที่คุณชอบ", result.question)
        assertEquals(2, result.options.size)
    }

    @Test
    fun testDeletePollAlsoDeletesOptions() {
        // Arrange: ตั้งค่า input และผลลัพธ์ที่คาดหวัง
        val pollId = PollRepository.getNextPollId()
        PollRepository.addPoll(Poll(pollId, "จะลบอันนี้"))
        val optionId = PollRepository.getNextOptionId()
        PollRepository.addPollOption(PollOption(optionId, "ตัวเลือกที่จะหายไป", votesCount = 0, pollId = pollId))

        // Act: เรียกใช้ฟังก์ชันที่ต้องการทดสอบ
        PollRepository.deletePoll(pollId)
        val pollAfterDelete = PollRepository.getPollById(pollId)
        val optionsAfterDelete = PollRepository.getOptionsforPoll(pollId)

        // Assert: ตรวจสอบว่าผลลัพธ์ที่ได้ตรงกับที่คาดหวัง
        assertNull(pollAfterDelete)
        assertTrue(optionsAfterDelete.isEmpty())
    }
}
