package com.call.videocallapplication.listeners;

import com.call.videocallapplication.models.User;

public interface UserListener {

    void initVideoMeeting(User user);

    void initAudioMeeting(User user);

    void onMultipleUserAction(Boolean isMultipleUsersSelected);
}
