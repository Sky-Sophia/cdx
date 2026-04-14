package org.example.propertyms.notification.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import org.example.propertyms.auth.dto.UserSession;
import org.example.propertyms.common.util.StringHelper;
import org.example.propertyms.notification.mapper.NotificationAudienceMapper;
import org.example.propertyms.notification.mapper.NotificationMapper;
import org.example.propertyms.notification.model.NotificationBatch;
import org.example.propertyms.notification.model.NotificationDepartment;
import org.example.propertyms.notification.model.NotificationDispatchResult;
import org.example.propertyms.notification.model.NotificationItem;
import org.example.propertyms.notification.model.NotificationMessage;
import org.example.propertyms.notification.model.NotificationSendPayload;
import org.example.propertyms.notification.model.NotificationTargetType;
import org.example.propertyms.user.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationServiceImpl implements NotificationService {
    private static final int DEFAULT_LIMIT = 30;
    private static final int MAX_LIMIT = 100;
    private static final DateTimeFormatter BATCH_TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final NotificationMapper notificationMapper;
    private final NotificationAudienceMapper notificationAudienceMapper;

    public NotificationServiceImpl(NotificationMapper notificationMapper,
                                   NotificationAudienceMapper notificationAudienceMapper) {
        this.notificationMapper = notificationMapper;
        this.notificationAudienceMapper = notificationAudienceMapper;
    }

    @Override
    public List<NotificationItem> loadInbox(Long receiverId, int limit) {
        int safeLimit = limit > 0 ? Math.min(limit, MAX_LIMIT) : DEFAULT_LIMIT;
        return notificationMapper.findInbox(receiverId, safeLimit).stream()
                .map(this::toItem)
                .toList();
    }

    @Override
    public int countUnread(Long receiverId) {
        return notificationMapper.countUnread(receiverId);
    }

    @Override
    @Transactional
    public List<NotificationDispatchResult> send(UserSession sender, NotificationSendPayload payload) {
        NotificationSendCommand command = buildSendCommand(sender, payload);
        List<User> recipients = resolveRecipients(sender, command.targetType(), command.rawReceiver());
        if (recipients.isEmpty()) {
            throw new IllegalArgumentException("没有匹配到可接收该通知的系统用户。");
        }

        LocalDateTime now = LocalDateTime.now();
        NotificationBatch batch = createBatch(sender.getId(), command, now);
        notificationMapper.insertBatch(batch);

        List<NotificationDispatchResult> results = new ArrayList<>(recipients.size());
        for (User recipient : recipients) {
            NotificationMessage message = createMessage(batch, command, sender, recipient, now);
            notificationMapper.insertMessage(message);
            results.add(new NotificationDispatchResult(recipient.getId(), toItem(message)));
        }
        return results;
    }

    @Override
    @Transactional
    public NotificationItem markRead(Long receiverId, Long notificationId) {
        return updateNotification(receiverId,
                notificationId,
                this::isRead,
                () -> notificationMapper.markRead(notificationId, receiverId));
    }

    @Override
    @Transactional
    public List<Long> markAllRead(Long receiverId) {
        requireReceiverId(receiverId);
        List<Long> unreadIds = notificationMapper.findUnreadIds(receiverId);
        if (!unreadIds.isEmpty()) {
            notificationMapper.markAllRead(receiverId);
        }
        return unreadIds;
    }

    @Override
    @Transactional
    public NotificationItem hidePopup(Long receiverId, Long notificationId) {
        return updateNotification(receiverId,
                notificationId,
                this::isPopupHidden,
                () -> notificationMapper.hidePopup(notificationId, receiverId));
    }

    @Override
    @Transactional
    public Long delete(Long receiverId, Long notificationId) {
        NotificationMessage current = requireActiveNotification(receiverId, notificationId);
        notificationMapper.delete(current.getId(), receiverId);
        return notificationId;
    }

    @Override
    @Transactional
    public List<Long> deleteAll(Long receiverId) {
        requireReceiverId(receiverId);
        List<Long> ids = notificationMapper.findInboxIds(receiverId);
        if (!ids.isEmpty()) {
            notificationMapper.deleteAll(receiverId);
        }
        return ids;
    }

    private NotificationSendCommand buildSendCommand(UserSession sender, NotificationSendPayload payload) {
        if (sender == null || sender.getId() == null) {
            throw new IllegalArgumentException("当前会话已失效，请重新登录。");
        }
        if (payload == null) {
            throw new IllegalArgumentException("通知内容不能为空。");
        }

        String msgType = StringHelper.trimToNull(payload.getMsgType());
        String content = StringHelper.trimToNull(payload.getContent());
        String rawReceiver = StringHelper.trimToNull(payload.getReceiver());
        NotificationTargetType targetType = NotificationTargetType.from(payload.getTargetType());

        if (msgType == null) {
            throw new IllegalArgumentException("消息类型不能为空。");
        }
        if (content == null) {
            throw new IllegalArgumentException("消息内容不能为空。");
        }
        return new NotificationSendCommand(msgType, content, rawReceiver, targetType);
    }

    private NotificationBatch createBatch(Long senderId,
                                          NotificationSendCommand command,
                                          LocalDateTime now) {
        NotificationBatch batch = new NotificationBatch();
        batch.setBatchNo(nextBatchNo(now));
        batch.setMsgType(command.msgType());
        batch.setContent(command.content());
        batch.setSenderId(senderId);
        batch.setTargetType(command.targetType().name());
        batch.setTargetValue(command.rawReceiver());
        batch.setCreatedAt(now);
        batch.setUpdatedAt(now);
        return batch;
    }

    private NotificationMessage createMessage(NotificationBatch batch,
                                              NotificationSendCommand command,
                                              UserSession sender,
                                              User recipient,
                                              LocalDateTime now) {
        NotificationMessage message = new NotificationMessage();
        message.setBatchId(batch.getId());
        message.setBatchNo(batch.getBatchNo());
        message.setMsgType(command.msgType());
        message.setContent(command.content());
        message.setSenderId(sender.getId());
        message.setSenderName(sender.getUsername());
        message.setReceiverId(recipient.getId());
        message.setReceiverName(recipient.getUsername());
        message.setSendTime(now);
        message.setIsRead(0);
        message.setIsDeleted(0);
        message.setTargetType(command.targetType().name());
        message.setTargetValue(command.rawReceiver());
        message.setCreatedAt(now);
        message.setUpdatedAt(now);
        return message;
    }

    private NotificationItem updateNotification(Long receiverId,
                                                Long notificationId,
                                                Predicate<NotificationMessage> skipCondition,
                                                Runnable updater) {
        NotificationMessage current = requireActiveNotification(receiverId, notificationId, "通知已被删除。");
        if (!skipCondition.test(current)) {
            updater.run();
            current = requireOwnedNotification(receiverId, notificationId);
        }
        return toItem(current);
    }

    private NotificationMessage requireActiveNotification(Long receiverId, Long notificationId) {
        return requireActiveNotification(receiverId, notificationId, "通知已被删除。");
    }

    private NotificationMessage requireActiveNotification(Long receiverId,
                                                          Long notificationId,
                                                          String deletedMessage) {
        NotificationMessage current = requireOwnedNotification(receiverId, notificationId);
        if (isDeleted(current)) {
            throw new IllegalArgumentException(deletedMessage);
        }
        return current;
    }

    private NotificationMessage requireOwnedNotification(Long receiverId, Long notificationId) {
        requireReceiverId(receiverId);
        if (notificationId == null) {
            throw new IllegalArgumentException("通知参数不完整。");
        }
        NotificationMessage message = notificationMapper.findByIdForReceiver(notificationId, receiverId);
        if (message == null) {
            throw new IllegalArgumentException("通知不存在或无权操作。");
        }
        return message;
    }

    private void requireReceiverId(Long receiverId) {
        if (receiverId == null) {
            throw new IllegalArgumentException("通知参数不完整。");
        }
    }

    private List<User> resolveRecipients(UserSession sender, NotificationTargetType targetType, String rawReceiver) {
        return deduplicateById(switch (targetType) {
            case SINGLE -> resolveSingleRecipient(rawReceiver);
            case ALL -> notificationAudienceMapper.findAllActiveUsers(sender.getId());
            case DEPARTMENT -> resolveDepartmentRecipients(sender.getId(), rawReceiver);
            case BUILDING -> resolveBuildingRecipients(sender.getId(), rawReceiver);
            case DUE_BILL -> notificationAudienceMapper.findActiveUsersByDueBill(sender.getId());
            case WORK_ORDER_DONE -> notificationAudienceMapper.findActiveUsersByCompletedWorkOrder(sender.getId());
        });
    }

    private List<User> resolveSingleRecipient(String rawReceiver) {
        if (rawReceiver == null) {
            throw new IllegalArgumentException("请输入接收人 ID 或用户名。");
        }
        User target = rawReceiver.chars().allMatch(Character::isDigit)
                ? notificationAudienceMapper.findActiveUserById(Long.parseLong(rawReceiver))
                : notificationAudienceMapper.findActiveUserByUsername(StringHelper.lowerCaseOrNull(rawReceiver));
        if (target == null) {
            throw new IllegalArgumentException("接收人不存在或账号未启用。");
        }
        return List.of(target);
    }

    private List<User> resolveDepartmentRecipients(Long senderId, String rawReceiver) {
        NotificationDepartment department = NotificationDepartment.from(rawReceiver);
        return notificationAudienceMapper.findActiveUsersByDepartment(
                department.getCode(),
                department.getFallbackRole(),
                senderId);
    }

    private List<User> resolveBuildingRecipients(Long senderId, String rawReceiver) {
        if (rawReceiver == null) {
            throw new IllegalArgumentException("请选择楼栋。");
        }
        return notificationAudienceMapper.findActiveUsersByBuilding(rawReceiver, senderId);
    }

    private List<User> deduplicateById(List<User> users) {
        Map<Long, User> unique = new LinkedHashMap<>();
        for (User user : users) {
            if (user != null && user.getId() != null) {
                unique.putIfAbsent(user.getId(), user);
            }
        }
        return new ArrayList<>(unique.values());
    }

    private NotificationItem toItem(NotificationMessage message) {
        NotificationItem item = new NotificationItem();
        item.setId(message.getId());
        item.setMsgType(message.getMsgType());
        item.setContent(message.getContent());
        item.setSender(message.getSenderName());
        item.setReceiver(message.getReceiverName());
        item.setTargetType(message.getTargetType());
        item.setTargetValue(message.getTargetValue());
        item.setSendTime(message.getSendTime());
        item.setRead(isRead(message));
        item.setReadTime(message.getReadTime());
        item.setPopupHidden(isPopupHidden(message));
        item.setPopupHiddenTime(message.getPopupHiddenTime());
        return item;
    }

    private boolean isDeleted(NotificationMessage message) {
        return isTrue(message.getIsDeleted());
    }

    private boolean isRead(NotificationMessage message) {
        return isTrue(message.getIsRead());
    }

    private boolean isPopupHidden(NotificationMessage message) {
        return isTrue(message.getIsPopupHidden());
    }

    private boolean isTrue(Integer value) {
        return value != null && value == 1;
    }

    private String nextBatchNo(LocalDateTime now) {
        return "NT" + BATCH_TS.format(now) + String.format("%02d", ThreadLocalRandom.current().nextInt(100));
    }

    private record NotificationSendCommand(String msgType,
                                           String content,
                                           String rawReceiver,
                                           NotificationTargetType targetType) {
    }
}
