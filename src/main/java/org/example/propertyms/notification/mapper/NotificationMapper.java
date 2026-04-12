package org.example.propertyms.notification.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Update;
import org.example.propertyms.notification.model.NotificationMessage;

@Mapper
public interface NotificationMapper {

    @Insert("""
            INSERT INTO notification_messages (
                batch_no, msg_type, content, sender_id, sender_name,
                receiver_id, receiver_name, send_time, is_read, read_time,
                is_deleted, deleted_time, target_type, target_value, created_at, updated_at
            ) VALUES (
                #{batchNo}, #{msgType}, #{content}, #{senderId}, #{senderName},
                #{receiverId}, #{receiverName}, #{sendTime}, #{isRead}, #{readTime},
                #{isDeleted}, #{deletedTime}, #{targetType}, #{targetValue}, #{createdAt}, #{updatedAt}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(NotificationMessage message);

    @Select("""
            SELECT *
            FROM notification_messages
            WHERE receiver_id = #{receiverId}
              AND is_deleted = 0
            ORDER BY is_read ASC, send_time DESC, id DESC
            LIMIT #{limit}
            """)
    List<NotificationMessage> findInbox(@Param("receiverId") Long receiverId, @Param("limit") int limit);

    @Select("""
            SELECT *
            FROM notification_messages
            WHERE id = #{id}
              AND receiver_id = #{receiverId}
            LIMIT 1
            """)
    NotificationMessage findByIdForReceiver(@Param("id") Long id, @Param("receiverId") Long receiverId);

    @Select("""
            SELECT id
            FROM notification_messages
            WHERE receiver_id = #{receiverId}
              AND is_deleted = 0
              AND is_read = 0
            ORDER BY send_time DESC, id DESC
            """)
    List<Long> findUnreadIds(@Param("receiverId") Long receiverId);

    @Select("""
            SELECT id
            FROM notification_messages
            WHERE receiver_id = #{receiverId}
              AND is_deleted = 0
            ORDER BY send_time DESC, id DESC
            """)
    List<Long> findInboxIds(@Param("receiverId") Long receiverId);

    @Update("""
            UPDATE notification_messages
            SET is_read = 1,
                read_time = COALESCE(read_time, CURRENT_TIMESTAMP),
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{id}
              AND receiver_id = #{receiverId}
              AND is_deleted = 0
              AND is_read = 0
            """)
    int markRead(@Param("id") Long id, @Param("receiverId") Long receiverId);

    @Update("""
            UPDATE notification_messages
            SET is_read = 1,
                read_time = COALESCE(read_time, CURRENT_TIMESTAMP),
                updated_at = CURRENT_TIMESTAMP
            WHERE receiver_id = #{receiverId}
              AND is_deleted = 0
              AND is_read = 0
            """)
    int markAllRead(@Param("receiverId") Long receiverId);

    @Delete("""
            DELETE FROM notification_messages
            WHERE id = #{id}
              AND receiver_id = #{receiverId}
            """)
    int delete(@Param("id") Long id, @Param("receiverId") Long receiverId);

    @Delete("""
            DELETE FROM notification_messages
            WHERE receiver_id = #{receiverId}
              AND is_deleted = 0
            """)
    int deleteAll(@Param("receiverId") Long receiverId);

    @Select("""
            SELECT COUNT(*)
            FROM notification_messages
            WHERE receiver_id = #{receiverId}
              AND is_deleted = 0
              AND is_read = 0
            """)
    int countUnread(@Param("receiverId") Long receiverId);
}
