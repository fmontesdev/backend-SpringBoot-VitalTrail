package com.springboot.vitaltrail.domain.notification;

import com.springboot.vitaltrail.api.notification.NotificationDto;

public interface NotificationService {
    void sendNotification(final NotificationDto notification);
}
