# Video Call Meeting Room API Guide

## Overview
This enhanced video call system allows mentors to create meeting rooms and generate shareable links. Students can join using the link, but require host approval before entering the actual meeting.

## Key Features
- ✅ **Room Creation**: Mentors create meetings with unique room IDs
- ✅ **Shareable Links**: Generate links that students can use to join
- ✅ **Host Approval**: Students wait in a "waiting room" until approved
- ✅ **Real-time Notifications**: WebSocket-based approval system
- ✅ **Security**: Role-based access and mentor-student relationship validation

## API Endpoints

### 1. Create Meeting Room (Mentor Only)
```http
POST /video-calls/create
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
  "mentorName": "John Doe",
  "title": "Java Programming Session",
  "description": "Weekly mentoring session",
  "scheduledTime": "2024-12-15T10:00:00",
  "maxParticipants": 10,
  "password": "optional-password"
}
```

**Response:**
```json
{
  "id": "meeting123",
  "roomId": "room_1702645200000_123",
  "joinLink": "/meeting/join/room_1702645200000_123",
  "title": "Java Programming Session",
  "status": "SCHEDULED",
  "requiresApproval": true
}
```

### 2. Get Shareable Meeting Link
```http
GET /video-calls/link/{roomId}
```

**Response:**
```json
{
  "joinLink": "/meeting/join/room_1702645200000_123",
  "roomId": "room_1702645200000_123"
}
```

### 3. Join Meeting (Student)
```http
POST /video-calls/join
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
  "roomId": "room_1702645200000_123",
  "participantName": "Jane Smith",
  "password": "optional-password"
}
```

**Response:** Student is added to waiting room, mentor gets notification

### 4. Approve Participant (Mentor Only)
```http
POST /video-calls/approve
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
  "roomId": "room_1702645200000_123",
  "participantId": "student123"
}
```

### 5. Get Waiting Participants (Mentor Only)
```http
GET /video-calls/waiting/{roomId}
Authorization: Bearer <jwt-token>
```

**Response:**
```json
["student123", "student456"]
```

### 6. Start Meeting (Mentor Only)
```http
POST /video-calls/start
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
  "roomId": "room_1702645200000_123"
}
```

## WebSocket Events

### Connection
```javascript
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);
```

### Subscribe to Join Requests (Mentor)
```javascript
stompClient.subscribe('/topic/meeting/' + roomId + '/join-requests', function(message) {
  const data = JSON.parse(message.body);
  // Show approval UI for data.participantId
});
```

### Subscribe to Approval Notifications (Student)
```javascript
stompClient.subscribe('/queue/user/' + userId + '/meeting-approval', function(message) {
  const data = JSON.parse(message.body);
  if (data.status === 'approved') {
    // Join the actual meeting
  }
});
```

## Frontend Integration

### For React Frontend

1. **Create Meeting (Mentor)**:
```javascript
const createMeeting = async (meetingData) => {
  const response = await fetch('/video-calls/create', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(meetingData)
  });
  return response.json();
};
```

2. **Generate Shareable Link**:
```javascript
const generateShareableLink = (roomId) => {
  return `${window.location.origin}/meeting-room.html?roomId=${roomId}`;
};
```

3. **WebRTC Integration**:
```javascript
// Use libraries like:
// - Simple-peer for basic WebRTC
// - Jitsi Meet API for full-featured solution
// - Daily.co API for hosted solution
// - Agora SDK for enterprise solution
```

## Recommended WebRTC Libraries

### Option 1: Jitsi Meet (Free & Open Source)
```html
<script src="https://meet.jit.si/external_api.js"></script>
<script>
const api = new JitsiMeetExternalAPI('meet.jit.si', {
  roomName: roomId,
  parentNode: document.querySelector('#video-container')
});
</script>
```

### Option 2: Daily.co (Hosted Solution)
```javascript
import DailyIframe from '@daily-co/daily-js';

const callFrame = DailyIframe.createFrame({
  iframeStyle: { width: '100%', height: '100%' },
  showLeaveButton: true
});

callFrame.join({ url: `https://your-domain.daily.co/${roomId}` });
```

### Option 3: Simple WebRTC (Custom Implementation)
```javascript
// Requires signaling server for peer discovery
// More complex but gives full control
```

## Security Considerations

1. **Authentication**: All endpoints require valid JWT tokens
2. **Authorization**: Role-based access (MENTOR vs STUDENT)
3. **Relationship Validation**: Students can only join meetings by their assigned mentor
4. **Password Protection**: Optional meeting passwords
5. **Waiting Room**: Host approval required before joining

## Database Schema

The `VideoCall` entity includes:
- `roomId`: Unique meeting identifier
- `joinLink`: Shareable link path
- `waitingParticipants`: List of users awaiting approval
- `requiresApproval`: Boolean flag for approval workflow
- `participants`: List of approved participants

## Next Steps

1. **Choose WebRTC Solution**: Select from Jitsi, Daily.co, or custom implementation
2. **Frontend Integration**: Implement the meeting UI in your React app
3. **Real-time Features**: Add WebSocket support for live notifications
4. **Testing**: Test the approval workflow end-to-end
5. **Production Setup**: Configure STUN/TURN servers for WebRTC

## Example Usage Flow

1. **Mentor creates meeting** → Gets `roomId` and `joinLink`
2. **Mentor shares link** → Students click the link
3. **Student joins** → Added to waiting room, mentor notified
4. **Mentor approves** → Student enters actual video call
5. **Video call begins** → WebRTC handles audio/video streaming

This backend provides the foundation for a secure, approval-based video meeting system. The actual video/audio streaming should be handled by a WebRTC library on the frontend.