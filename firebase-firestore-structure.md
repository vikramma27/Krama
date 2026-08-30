# Firestore Database Structure

## Overview

This document describes the Firestore database structure for Krama messaging app, following security-first design principles.

## Database Collections

### 1. users/{userId}

User profile information and public data.

**Fields:**
```json
{
  "uid": "string",           // Firebase Auth UID (required)
  "email": "string",         // Email address
  "displayName": "string",   // Display name
  "photoURL": "string",      // Profile photo URL
  "phoneNumber": "string",   // Phone number (optional)
  "createdAt": "timestamp",  // Account creation date
  "lastSeen": "timestamp",   // Last online timestamp
  "isOnline": "boolean",    // Online status
  "bio": "string",          // User bio (optional)
  "settings": {             // Public settings
    "publicProfile": "boolean",
    "showLastSeen": "boolean",
    "showOnlineStatus": "boolean"
  }
}
```

**Subcollection:** users/{userId}/private
- Private user data accessible only by the user
- Contains sensitive information like encryption keys

### 2. conversations/{conversationId}

Chat conversations and group chats.

**Fields:**
```json
{
  "conversationId": "string",      // Unique conversation ID
  "type": "string",                // "direct" or "group"
  "participants": ["uid1", "uid2"], // Array of participant UIDs
  "participantNames": {            // Display names for participants
    "uid1": "Name 1",
    "uid2": "Name 2"
  },
  "lastMessage": {                  // Last message preview
    "text": "string",
    "senderId": "string",
    "timestamp": "timestamp"
  },
  "lastMessageTime": "timestamp",   // For sorting
  "unreadCount": {                 // Unread counts per user
    "uid1": 0,
    "uid2": 5
  },
  "createdAt": "timestamp",
  "updatedAt": "timestamp",
  "isArchived": "boolean",         // Archive status
  "isPinned": "boolean",          // Pinned conversations
  "isMuted": "boolean"            // Muted conversations
}
```

**Subcollection:** conversations/{conversationId}/messages/{messageId}

Individual messages within a conversation.

**Fields:**
```json
{
  "messageId": "string",
  "conversationId": "string",
  "senderId": "string",
  "senderName": "string",
  "text": "string",
  "type": "string",              // "text", "image", "video", "audio", "file"
  "mediaUrl": "string",         // URL for media content
  "mediaThumbnail": "string",   // Thumbnail URL
  "mediaSize": "number",        // File size in bytes
  "replyTo": "string",          // Parent message ID for replies
  "replyToText": "string",      // Original message text (for preview)
  "reactions": [                // Message reactions
    {
      "userId": "string",
      "emoji": "string",
      "timestamp": "timestamp"
    }
  ],
  "status": "string",           // "sent", "delivered", "read"
  "isDeleted": "boolean",      // Soft delete flag
  "isEdited": "boolean",       // Edited flag
  "editedAt": "timestamp",     // Edit timestamp
  "createdAt": "timestamp"
}
```

### 3. contacts/{userId}

User's contact list and friend requests.

**Fields:**
```json
{
  "userId": "string",
  "contacts": [
    {
      "contactId": "string",
      "displayName": "string",
      "photoURL": "string",
      "addedAt": "timestamp"
    }
  ]
}
```

**Subcollection:** contacts/{userId}/requests/{requestId}

Friend/contact requests.

**Fields:**
```json
{
  "requestId": "string",
  "fromUserId": "string",
  "fromUserName": "string",
  "fromUserPhoto": "string",
  "status": "string",          // "pending", "accepted", "rejected"
  "message": "string",         // Request message
  "createdAt": "timestamp",
  "updatedAt": "timestamp"
}
```

### 4. settings/{userId}

User preferences and app settings.

**Fields:**
```json
{
  "userId": "string",
  "notifications": {
    "enabled": "boolean",
    "sound": "boolean",
    "vibration": "boolean",
    "showPreviews": "boolean"
  },
  "privacy": {
    "lastSeen": "string",      // "everyone", "contacts", "nobody"
    "onlineStatus": "string",  // "everyone", "contacts", "nobody"
    "readReceipts": "boolean",
    "profilePhoto": "string",  // "everyone", "contacts", "nobody"
  },
  "theme": "string",           // "light", "dark", "system"
  "language": "string",        // ISO language code
  "messageFontSize": "number",  // 12-24
}
```

### 5. blocked/{userId}

Blocked users list.

**Fields:**
```json
{
  "userId": "string",
  "blockedUsers": [
    {
      "blockedUserId": "string",
      "blockedAt": "timestamp",
      "reason": "string"
    }
  ]
}
```

## Indexes

Create the following composite indexes in Firestore:

### Required Indexes

1. **conversations** collection:
   - Field: `participants` (ASC)
   - Field: `lastMessageTime` (DESC)

2. **messages** subcollection:
   - Field: `conversationId` (ASC)
   - Field: `createdAt` (DESC)

3. **users** collection:
   - Field: `email` (ASC)

### Recommended Indexes

1. **contacts** collection:
   - Field: `userId` (ASC)
   - Field: `contacts.addedAt` (DESC)

2. **messages** with status:
   - Field: `conversationId` (ASC)
   - Field: `status` (ASC)
   - Field: `createdAt` (DESC)

## Data Retention

### Message Retention Policy (Free Tier Compatible)

- **Active messages:** Keep indefinitely
- **Deleted messages:** Soft delete (mark as deleted, retain for 30 days)
- **Media attachments:** Auto-delete after 90 days
- **Old conversations:** Archive after 1 year of inactivity

### Cost Optimization

- Use Firebase Auth for user data (free tier: 10K monthly active users)
- Firestore free tier: 1GB storage, 50K reads, 20K writes, 20K deletes per day
- Use Firestore bundled queries to reduce read costs
- Implement pagination for message history

## Security Rules

See `firestore.rules` for complete security rule implementation.

## Best Practices

1. **Denormalization:** Duplicate necessary data to reduce queries
2. **Batch Operations:** Use batch writes for multi-document updates
3. **Offline Support:** Enable Firestore offline persistence
4. **Real-time Listeners:** Use for active conversations only
5. **Query Limits:** Always use limits on large collections
6. **Index Optimization:** Create composite indexes for common queries
