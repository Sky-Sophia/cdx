package org.example.propertyms.notification.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Update;
import org.example.propertyms.notification.model.NotificationBatch;
import org.example.propertyms.notification.model.NotificationMessage;

@Mapper
public interface NotificationMapper {
    String MESSAGE_SELECT_COLUMNS = """
            nm.id,
            nm.batch_id,
            nb.batch_no,
            nb.msg_type,
            nb.content,
            nb.sender_id,
            COALESCE(sp.full_name, su.username) AS sender_name,
            nm.receiver_id,
            COALESCE(rp.full_name, ru.username) AS receiver_name,
            nm.send_time,
            nm.is_read,
            nm.read_time,
            nm.is_deleted,
            nm.deleted_time,
            nm.is_popup_hidden,
            nm.popup_hidden_time,
            nb.target_type,
            nb.target_value,
            nm.created_at,
            nm.updated_at
            FROM notification_messages nm
            INNER JOIN notification_batches nb ON nb.id = nm.batch_id
            LEFT JOIN user_accounts su ON su.id = nb.sender_id
            LEFT JOIN persons sp ON sp.id = su.person_id
            LEFT JOIN user_accounts ru ON ru.id = nm.receiver_id
            LEFT JOIN persons rp ON rp.id = ru.person_id
            """;

    @Insert("""
            INSERT INTO notification_batches (
                batch_no, msg_type, content, sender_id, target_type, target_value, created_at, updated_at
            ) VALUES (
                #{batchNo}, #{msgType}, #{content}, #{senderId}, #{targetType}, #{targetValue}, #{createdAt}, #{updatedAt}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertBatch(NotificationBatch batch);

    @Insert("""
            INSERT INTO notification_messages (
                batch_id, receiver_id, send_time, is_read, read_time,
                is_deleted, deleted_time, created_at, updated_at
            ) VALUES (
                #{batchId}, #{receiverId}, #{sendTime}, #{isRead}, #{readTime},
                #{isDeleted}, #{deletedTime}, #{createdAt}, #{updatedAt}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertMessage(NotificationMessage message);

    @Select("""
            SELECT
            """ + MESSAGE_SELECT_COLUMNS + """
            WHERE nm.receiver_id = #{receiverId}
              AND nm.is_deleted = 0
            ORDER BY nm.is_read, nm.send_time DESC, nm.id DESC
            LIMIT #{limit}
            """)
    List<NotificationMessage> findInbox(@Param("receiverId") Long receiverId, @Param("limit") int limit);

    @Select("""
            SELECT
            """ + MESSAGE_SELECT_COLUMNS + """
            WHERE nm.id = #{id}
              AND nm.receiver_id = #{receiverId}
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
    void markRead(@Param("id") Long id, @Param("receiverId") Long receiverId);

    @Update("""
            UPDATE notification_messages
            SET is_read = 1,
                read_time = COALESCE(read_time, CURRENT_TIMESTAMP),
                updated_at = CURRENT_TIMESTAMP
            WHERE receiver_id = #{receiverId}
              AND is_deleted = 0
              AND is_read = 0
            """)
    void markAllRead(@Param("receiverId") Long receiverId);

    @Update("""
            UPDATE notification_messages
            SET is_popup_hidden = 1,
                popup_hidden_time = COALESCE(popup_hidden_time, CURRENT_TIMESTAMP),
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{id}
              AND receiver_id = #{receiverId}
              AND is_deleted = 0
              AND is_popup_hidden = 0
            """)
    void hidePopup(@Param("id") Long id, @Param("receiverId") Long receiverId);

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
    void deleteAll(@Param("receiverId") Long receiverId);

    @Select("""
            SELECT COUNT(*)
            FROM notification_messages
            WHERE receiver_id = #{receiverId}
              AND is_deleted = 0
              AND is_read = 0
            """)
    int countUnread(@Param("receiverId") Long receiverId);
}

