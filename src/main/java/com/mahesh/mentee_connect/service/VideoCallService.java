package com.mahesh.mentee_connect.service;

import com.mahesh.mentee_connect.dto.VideoCallDtos.AdminVideoCallDTO;
import com.mahesh.mentee_connect.dto.VideoCallDtos.MentorVideoCallDTO;
import com.mahesh.mentee_connect.dto.VideoCallDtos.VideoCallStartRequest;
import com.mahesh.mentee_connect.model.VideoCallRecord;

import java.util.List;

public interface VideoCallService {

    VideoCallRecord startCall(VideoCallStartRequest request);

    VideoCallRecord endCall(String mentorId, String callId);

    List<AdminVideoCallDTO> getAllForAdmin();

    List<MentorVideoCallDTO> getForMentor(String mentorId);
}

