package org.example.propertyms.bill.mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.Update;
import org.example.propertyms.bill.model.FeeBill;

@Mapper
public interface FeeBillMapper {

    @SelectProvider(type = FeeBillSqlProvider.class, method = "countSql")
    long count(@Param("keyword") String keyword,
               @Param("status") String status,
               @Param("billingMonth") String billingMonth);

    @SelectProvider(type = FeeBillSqlProvider.class, method = "findAllPagedSql")
    List<FeeBill> findAllPaged(@Param("keyword") String keyword,
                                @Param("status") String status,
                                @Param("billingMonth") String billingMonth,
                                @Param("offset") int offset,
                                @Param("pageSize") int pageSize);

    @SelectProvider(type = FeeBillSqlProvider.class, method = "findAllSql")
    List<FeeBill> findAll(@Param("keyword") String keyword,
                          @Param("status") String status,
                          @Param("billingMonth") String billingMonth);

    @Select("SELECT f.*, u.unit_no FROM fee_bills f LEFT JOIN units u ON u.id = f.unit_id WHERE f.id = #{id}")
    FeeBill findById(@Param("id") Long id);

    @Select("SELECT f.*, u.unit_no FROM fee_bills f LEFT JOIN units u ON u.id = f.unit_id WHERE f.status <> 'PAID' ORDER BY f.due_date LIMIT #{limit}")
    List<FeeBill> findDueSoon(@Param("limit") int limit);

    @Insert("""
            INSERT INTO fee_bills (bill_no, unit_id, billing_month, amount, paid_amount, status, due_date, remarks)
            VALUES (#{billNo}, #{unitId}, #{billingMonth}, #{amount}, #{paidAmount}, #{status}, #{dueDate}, #{remarks})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(FeeBill bill);

    @Update("""
            UPDATE fee_bills
            SET paid_amount = #{paidAmount},
                status = #{status},
                paid_at = #{paidAt},
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{id}
            """)
    void updatePayment(@Param("id") Long id,
                       @Param("paidAmount") BigDecimal paidAmount,
                       @Param("status") String status,
                       @Param("paidAt") LocalDateTime paidAt);

    @Select("SELECT COUNT(*) FROM fee_bills WHERE status IN ('UNPAID', 'PARTIAL', 'OVERDUE')")
    long countDue();

    @Select("SELECT COALESCE(SUM(amount), 0) FROM fee_bills")
    BigDecimal sumReceivable();

    @Select("SELECT COALESCE(SUM(paid_amount), 0) FROM fee_bills")
    BigDecimal sumReceived();
}

