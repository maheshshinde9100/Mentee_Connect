# Video Call Feature - Frontend Implementation Instructions

## ✅ Backend Status: COMPLETE
The backend is fully implemented with all necessary APIs, WebSocket support, security, and approval workflow. You can now proceed with frontend development.

## 🎯 Frontend Requirements Overview

### Core Features to Implement:
1. **Meeting Creation Interface** (Mentor only)
2. **Meeting Join Interface** (Student)
3. **Waiting Room System** (Both roles)
4. **Video Call Interface** (WebRTC integration)
5. **Real-time Notifications** (WebSocket)
6. **Meeting Management** (Controls, participant list)

---

## 📋 Detailed Implementation Steps

### STEP 1: Meeting Creation Page (Mentor Dashboard)

**Purpose**: Allow mentors to create new video meetings and generate shareable links.

**UI Components Needed**:
- Form with fields: Title, Description, Scheduled Time, Max Participants, Password (optional)
- "Create Meeting" button
- Success modal showing generated room ID and shareable link
- Copy link button
- List of created meetings with status

**API Integration**:
- **Endpoint**: `POST /video-calls/create`
- **Headers**: `Authorization: Bearer <jwt-token>`, `Content-Type: application/json`
- **Request Body**: Meeting details (title, description, scheduledTime, maxParticipants, password)
- **Response**: Meeting object with roomId and joinLink

**User Flow**:
1. Mentor fills meeting form
2. Clicks "Create Meeting"
3. System calls API and shows success message
4. Mentor copies shareable link to send to students
5. Meeting appears in mentor's meeting list

---

### STEP 2: Meeting Join Interface (Student)

**Purpose**: Allow students to join meetings using shared links.

**UI Components Needed**:
- Join meeting form (room ID input, name input, password input if required)
- "Join Meeting" button
- Waiting room screen with "Waiting for host approval..." message
- Loading states and error handling

**API Integration**:
- **Endpoint**: `POST /video-calls/join`
- **Headers**: `Authorization: Bearer <jwt-token>`, `Content-Type: application/json`
- **Request Body**: `{roomId, participantName, password}`
- **Response**: Success/error message

**User Flow**:
1. Student clicks shared link or enters room ID manually
2. Enters their name and password (if required)
3. Clicks "Join Meeting"
4. System shows "Waiting for approval..." screen
5. Student waits for mentor approval notification

---

### STEP 3: WebSocket Integration for Real-time Notifications

**Purpose**: Enable real-time communication between mentor and students for approval workflow.

**Required Libraries**:
- `sockjs-client` for WebSocket connection
- `@stomp/stompjs` for STOMP protocol

**WebSocket Setup**:
- **Connection URL**: `/ws` (your backend WebSocket endpoint)
- **Protocols**: SockJS + STOMP

**Subscription Channels**:

**For Mentors**:
- **Channel**: `/topic/meeting/{roomId}/join-requests`
- **Purpose**: Receive notifications when students request to join
- **Message Format**: `{participantId, participantName, action: "join_request"}`

**For Students**:
- **Channel**: `/queue/user/{userId}/meeting-approval`
- **Purpose**: Receive approval/rejection notifications
- **Message Format**: `{roomId, status: "approved" | "rejected"}`

**Implementation Requirements**:
1. Establish WebSocket connection on meeting page load
2. Subscribe to appropriate channels based on user role
3. Handle incoming messages and update UI accordingly
4. Send messages when actions occur (join requests, approvals)

---

### STEP 4: Waiting Room & Approval System (Mentor Interface)

**Purpose**: Allow mentors to see and manage students waiting to join.

**UI Components Needed**:
- "Waiting Room" panel showing list of waiting participants
- Each participant item with "Approve" and "Reject" buttons
- Real-time updates when new join requests arrive
- Notification badges/sounds for new requests

**API Integration**:
- **Get Waiting**: `GET /video-calls/waiting/{roomId}`
- **Approve**: `POST /video-calls/approve` with `{roomId, participantId}`
- **Reject**: `POST /video-calls/reject` with `{roomId, participantId}`

**User Flow**:
1. Mentor sees notification when student requests to join
2. Student appears in waiting room list
3. Mentor clicks "Approve" or "Reject"
4. System updates participant lists
5. Student receives approval/rejection notification

---

### STEP 5: Video Call Interface (WebRTC Integration)

**Purpose**: Actual video/audio communication between participants.

**Recommended WebRTC Solutions**:

**Option A: Jitsi Meet (Free, Open Source)**
- **Library**: Jitsi Meet External API
- **CDN**: `https://meet.jit.si/external_api.js`
- **Pros**: Free, feature-rich, easy integration
- **Cons**: Limited customization

**Option B: Daily.co (Hosted Solution)**
- **Library**: `@daily-co/daily-js`
- **Pros**: Easy integration, good documentation, reliable
- **Cons**: Paid service after free tier

**Option C: Agora SDK (Enterprise)**
- **Library**: `agora-rtc-sdk-ng`
- **Pros**: High quality, scalable, lots of features
- **Cons**: More complex, paid service

**Integration Requirements**:
1. Initialize WebRTC library when user is approved to join
2. Use the `roomId` as the meeting room identifier
3. Implement video controls (camera, microphone, screen share)
4. Handle participant joining/leaving events
5. Integrate with your existing UI design

---

### STEP 6: Meeting Management Features

**Purpose**: Provide controls and information during active meetings.

**UI Components Needed**:
- Video control buttons (camera on/off, microphone on/off, screen share)
- Participant list with names and status
- Meeting info display (title, time, room ID)
- "End Meeting" button (mentor only)
- "Leave Meeting" button
- Chat functionality (optional)

**API Integration**:
- **Start Meeting**: `POST /video-calls/start` with `{roomId}`
- **End Meeting**: `POST /video-calls/end` with `{roomId}`
- **Leave Meeting**: `POST /video-calls/leave` with `{roomId}`
- **Get Meeting Details**: `GET /video-calls/room/{roomId}`

---

## 🔗 Complete API Endpoints Reference

### Authentication Required for All Endpoints
**Header**: `Authorization: Bearer <jwt-token>`

### Meeting Management APIs:
1. **Create Meeting**: `POST /video-calls/create`
2. **Get Meeting Details**: `GET /video-calls/room/{roomId}`
3. **Get Shareable Link**: `GET /video-calls/link/{roomId}`
4. **Join Meeting**: `POST /video-calls/join`
5. **Start Meeting**: `POST /video-calls/start` (Mentor only)
6. **End Meeting**: `POST /video-calls/end` (Mentor only)
7. **Leave Meeting**: `POST /video-calls/leave`

### Approval System APIs:
8. **Get Waiting Participants**: `GET /video-calls/waiting/{roomId}` (Mentor only)
9. **Approve Participant**: `POST /video-calls/approve` (Mentor only)
10. **Reject Participant**: `POST /video-calls/reject` (Mentor only)

### Meeting Lists APIs:
11. **Get Mentor's Meetings**: `GET /video-calls/mentor/{mentorId}`
12. **Get Available Meetings**: `GET /video-calls/available`
13. **Get Student's Meetings**: `GET /video-calls/mentee/{menteeId}`

---

## 🚀 Implementation Priority Order

### Phase 1: Basic Meeting Flow
1. Create meeting form (mentor)
2. Join meeting form (student)
3. Basic API integration
4. Simple success/error handling

### Phase 2: Approval System
1. WebSocket connection setup
2. Waiting room interface
3. Approval/rejection functionality
4. Real-time notifications

### Phase 3: Video Integration
1. Choose and integrate WebRTC solution
2. Video call interface
3. Meeting controls
4. Participant management

### Phase 4: Enhanced Features
1. Meeting history
2. Advanced controls
3. Chat functionality
4. Recording (if needed)

---

## 🔒 Security Considerations for Frontend

1. **JWT Token Management**: Store securely, refresh when needed
2. **Role-based UI**: Show/hide features based on user role (MENTOR/STUDENT)
3. **Input Validation**: Validate all form inputs before API calls
4. **Error Handling**: Graceful handling of network errors and API failures
5. **WebSocket Security**: Ensure proper authentication for WebSocket connections

---

## 📱 Responsive Design Requirements

1. **Mobile Support**: Ensure video calls work on mobile devices
2. **Tablet Layout**: Optimize for tablet screens
3. **Desktop Experience**: Full-featured interface for desktop users
4. **Cross-browser**: Test on Chrome, Firefox, Safari, Edge

---

## 🧪 Testing Requirements

1. **Unit Tests**: Test individual components and API integrations
2. **Integration Tests**: Test complete user flows
3. **Cross-browser Testing**: Ensure WebRTC works across browsers
4. **Mobile Testing**: Test video calls on mobile devices
5. **Network Testing**: Test with poor network conditions

---

## 📊 Analytics & Monitoring (Optional)

1. **Meeting Analytics**: Track meeting duration, participants
2. **Error Tracking**: Monitor API failures and WebRTC issues
3. **User Behavior**: Track feature usage and user flows
4. **Performance**: Monitor video call quality and connection issues

---

## 🎨 UI/UX Recommendations

1. **Loading States**: Show loading indicators during API calls
2. **Error Messages**: Clear, user-friendly error messages
3. **Success Feedback**: Confirm successful actions
4. **Intuitive Navigation**: Easy-to-understand meeting flow
5. **Accessibility**: Support for screen readers and keyboard navigation

---

## 📞 Support & Documentation

1. **User Guide**: Create help documentation for mentors and students
2. **Troubleshooting**: Common issues and solutions
3. **Browser Requirements**: List supported browsers and versions
4. **System Requirements**: Minimum hardware/software requirements

---

This comprehensive guide provides everything needed for frontend implementation. The backend is complete and ready to support all these features. Focus on implementing in phases, starting with basic meeting creation and joining, then adding the approval system and finally integrating WebRTC for actual video calls.