package com.example.data

import kotlinx.coroutines.flow.Flow

class AppRepository(
    private val meetingDao: MeetingDao,
    private val recipientDao: RecipientDao,
    private val chatDao: ChatDao
) {
    // Chat messages
    val allChatMessages: Flow<List<ChatMessage>> = chatDao.getAllMessages()

    suspend fun insertChatMessage(message: ChatMessage): Long {
        return chatDao.insertMessage(message)
    }

    suspend fun deleteChatMessageById(id: Int) {
        chatDao.deleteMessageById(id)
    }

    suspend fun clearAllChatMessages() {
        chatDao.clearAllMessages()
    }

    // Meetings
    val allMeetings: Flow<List<Meeting>> = meetingDao.getAllMeetings()

    suspend fun getMeetingById(id: Int): Meeting? {
        return meetingDao.getMeetingById(id)
    }

    suspend fun insertMeeting(meeting: Meeting): Long {
        return meetingDao.insertMeeting(meeting)
    }

    suspend fun updateMeeting(meeting: Meeting) {
        meetingDao.updateMeeting(meeting)
    }

    suspend fun deleteMeeting(meeting: Meeting) {
        meetingDao.deleteMeeting(meeting)
    }

    suspend fun deleteMeetingById(id: Int) {
        meetingDao.deleteMeetingById(id)
    }

    // Recipients
    val allRecipients: Flow<List<Recipient>> = recipientDao.getAllRecipients()

    suspend fun insertRecipient(recipient: Recipient): Long {
        return recipientDao.insertRecipient(recipient)
    }

    suspend fun updateRecipient(recipient: Recipient) {
        recipientDao.updateRecipient(recipient)
    }

    suspend fun deleteRecipient(recipient: Recipient) {
        recipientDao.deleteRecipient(recipient)
    }

    suspend fun deleteRecipientById(id: Int) {
        recipientDao.deleteRecipientById(id)
    }
}
