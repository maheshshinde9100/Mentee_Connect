# 🎥 Video Call WebSocket Testing Guide

## ✅ Updated Endpoints

### 1. Join Meeting Endpoint
```
POST /api/video-calls/join
Content-Type: application/json

{
  "roomId": "room_1760282409746_592",
  "participantName": "mahesh",
  "participantId": "68296e16231e576c65c8ed26",
  "password": "123"
}

Response:
{
  "success": true,
  "message": "Join request sent successfully",
  "status": "waiting_for_approval"
}
```

### 2. Approve Participant Endpoint
```
POST /api/video-calls/approve
Content-Type: application/json

{
  "roomId": "room_1760282409746_592",
  "participantId": "68296e16231e576c65c8ed26"
}

Response:
{
  "success": true,
  "message": "Participant approved successfully",
  "videoCall": { ... }
}
```

### 3. Test WebSocket Endpoint
```
POST /api/video-calls/test-websocket
Content-Type: application/json

{
  "roomId": "room_1760282409746_592",
  "participantId": "68296e16231e576c65c8ed26",
  "participantName": "mahesh"
}
```

## 🔄 WebSocket Flow

### Step 1: Student Joins Meeting
1. Student calls `POST /api/video-calls/join`
2. Backend adds student to waiting list
3. **WebSocket notification sent to mentor:**
   - Topic: `/topic/meeting/{roomId}/join-requests`
   - Message:
   ```json
   {
     "action": "join_request",
     "participantId": "68296e16231e576c65c8ed26",
     "participantName": "mahesh",
     "roomId": "room_1760282409746_592",
     "timestamp": "2024-12-12T10:30:00Z"
   }
   ```

### Step 2: Mentor Approves Student
1. Mentor calls `POST /api/video-calls/approve`
2. Backend moves student from waiting to participants
3. **WebSocket notification sent to student:**
   - Topic: `/queue/user/{participantId}/meeting-approval`
   - Message:
   ```json
   {
     "status": "approved",
     "roomId": "room_1760282409746_592",
     "meetingTitle": "Meeting Title",
     "message": "You have been approved to join the meeting",
     "joinLink": "/meeting/join/room_1760282409746_592"
   }
   ```

## 🧪 Testing Steps

### 1. Test WebSocket Connection
```javascript
// Frontend WebSocket connection
const socket = new SockJS('http://localhost:8080/api/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    console.log('Connected: ' + frame);
    
    // Subscribe to mentor notifications
    stompClient.subscribe('/topic/meeting/room_1760282409746_592/join-requests', function(message) {
        console.log('Join request received:', JSON.parse(message.body));
    });
    
    // Subscribe to student approval notifications
    stompClient.subscribe('/queue/user/68296e16231e576c65c8ed26/meeting-approval', function(message) {
        console.log('Approval received:', JSON.parse(message.body));
    });
});
```

### 2. Test Join Request
```bash
curl -X POST http://localhost:8080/api/video-calls/join \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "roomId": "room_1760282409746_592",
    "participantName": "mahesh",
    "participantId": "68296e16231e576c65c8ed26",
    "password": "123"
  }'
```

### 3. Test WebSocket Notification
```bash
curl -X POST http://localhost:8080/api/video-calls/test-websocket \
  -H "Content-Type: application/json" \
  -d '{
    "roomId": "room_1760282409746_592",
    "participantId": "68296e16231e576c65c8ed26",
    "participantName": "mahesh"
  }'
```

### 4. Test Approval
```bash
curl -X POST http://localhost:8080/api/video-calls/approve \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer MENTOR_JWT_TOKEN" \
  -d '{
    "roomId": "room_1760282409746_592",
    "participantId": "68296e16231e576c65c8ed26"
  }'
```

## 🔍 Debugging Checklist

### If WebSocket notifications aren't working:

1. **Check WebSocket Connection:**
   - Verify frontend connects to `ws://localhost:8080/api/ws`
   - Check browser console for connection errors

2. **Check Subscriptions:**
   - Mentor should subscribe to: `/topic/meeting/{roomId}/join-requests`
   - Student should subscribe to: `/queue/user/{userId}/meeting-approval`

3. **Check Backend Logs:**
   - Look for WebSocket connection logs
   - Check for `convertAndSend` calls in logs

4. **Test with Postman/Browser:**
   - Use the test endpoint to verify WebSocket is working
   - Check network tab for WebSocket frames

### If Video Call doesn't open:

1. **Check Meeting Status:**
   - Verify meeting status is "LIVE" after approval
   - Check if participant is in the participants list

2. **Check Frontend Logic:**
   - Ensure frontend opens video call after receiving approval
   - Verify the joinLink is being used correctly

3. **Check Video Call Integration:**
   - Verify Jitsi Meet or video call library is properly integrated
   - Check if room ID is passed correctly to video call component

## 🚀 Expected Behavior

1. **Student joins** → Gets "waiting_for_approval" response
2. **Mentor receives** → Real-time join request notification
3. **Mentor approves** → Student gets real-time approval notification
4. **Student's frontend** → Should automatically open video call
5. **Both participants** → Should see each other in the video call

## 📝 Notes

- Make sure `requiresApproval` is set to `true` in VideoCall entity
- WebSocket notifications are sent automatically when using the service methods
- The approval process moves participants from waiting list to active participants
- Video call should open automatically on the frontend after approval notification

Your backend is now fully configured for real-time video call management! 🎉