from __future__ import annotations

import argparse
from dataclasses import dataclass
from datetime import date, datetime, time, timedelta
from decimal import Decimal, ROUND_HALF_UP
from pathlib import Path
from typing import Iterable

AREA_CODE = "330108"  # 浙江省杭州市滨江区（合成示例）
WEIGHTS = [7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2]
CHECK_CODES = ["1", "0", "X", "9", "8", "7", "6", "5", "4", "3", "2"]

BUILDING_COUNT = 12
UNITS_PER_BUILDING = 20
TOTAL_UNITS = BUILDING_COUNT * UNITS_PER_BUILDING
NON_VACANT_UNITS = 180
TARGET_TOTAL_ROWS = 1000

PASSWORD_HASHES = {
    "admin": "{bcrypt}$2a$12$FCYzNs4BZpzwwxMXPsSzLuu4YPxApxOAQHxlDuIt1LrMdBnVgqjc.",
    "manager": "{bcrypt}$2a$12$pUighS26gH.QWaRALc/TJORaatu/J1T5tDcDDxHCEZXOwjygBU7z.",
    "finance": "{bcrypt}$2a$12$462yRqPtNqSSWMv3nXC/iuTOnmvtk.M9GeZoRDedSezX3Fw.LEXDO",
}

SURNAMES = [
    "王", "李", "张", "刘", "陈", "杨", "黄", "赵", "周", "吴",
    "徐", "孙", "朱", "马", "胡", "郭", "何", "高", "林", "罗",
    "郑", "梁", "谢", "宋", "唐", "许", "韩", "冯", "邓", "曹",
    "彭", "曾", "肖", "田", "董", "袁", "潘", "于", "蒋", "蔡",
]

GIVEN_FIRST = [
    "嘉", "宇", "思", "欣", "子", "梓", "俊", "文", "晨", "雨",
    "奕", "浩", "泽", "若", "依", "安", "景", "昊", "可", "心",
    "天", "沐", "彦", "清", "书", "雅", "梦", "亦", "承", "一",
]

GIVEN_SECOND = [
    "轩", "涵", "宁", "妍", "彤", "睿", "辰", "博", "菲", "琳",
    "怡", "婷", "洋", "雯", "琪", "豪", "楠", "峰", "鹏", "媛",
    "霖", "杰", "钰", "恒", "瑶", "然", "涛", "昕", "远", "敏",
]

OWNER_NOTES = [
    "物业服务费按月计收，已含公区照明与保洁。",
    "本月含公共能耗分摊费用。",
    "账单已同步至业主微信群与物业管家。",
    "支持物业前台、微信或银行转账缴费。",
]

WORK_ORDER_CATEGORIES = [
    "水电维修", "门禁故障", "管道疏通", "电梯报修", "照明维修", "空调检修", "弱电网络", "公共区域维护",
]

WORK_ORDER_TEMPLATES = {
    "水电维修": "住户反映厨房水槽下方有持续渗水，已影响橱柜底板，请尽快安排上门检修。",
    "门禁故障": "单元门禁识别不稳定，门禁卡偶发无法刷开，晚间出入受影响。",
    "管道疏通": "卫生间地漏排水缓慢，伴随轻微返味，需安排师傅排查并疏通。",
    "电梯报修": "高峰时段电梯到站后关门反应偏慢，住户担心影响通行效率。",
    "照明维修": "楼道感应灯频繁闪烁，夜间照明不足，建议尽快更换灯具。",
    "空调检修": "客厅空调制冷效果下降，运行噪声偏大，需要安排检测。",
    "弱电网络": "入户弱电箱线路松动，网络间歇性掉线，请协助排查。",
    "公共区域维护": "楼层消防通道附近墙面有轻微渗水痕迹，需要物业现场查看。",
}

ASSIGNEES = ["陈师傅", "李师傅", "王师傅", "赵师傅", "周管家", "徐工", "郭工", "沈师傅"]
PHONE_PREFIXES = ["139", "138", "137", "136", "135", "159", "158", "157", "188", "187", "186", "177", "176", "199"]


@dataclass(frozen=True)
class UnitRecord:
    id: int
    building_id: int
    unit_no: str
    floor_no: int
    area_m2: Decimal
    occupancy_status: str
    owner_name: str
    owner_phone: str
    created_at: datetime


@dataclass(frozen=True)
class ResidentProfile:
    unit_id: int
    primary_name: str
    primary_phone: str


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_OUTPUT = ROOT / "src" / "main" / "resources" / "db" / "realistic-property-seed.sql"


def make_name(seed: int, surname: str | None = None) -> str:
    family_name = surname or SURNAMES[seed % len(SURNAMES)]
    first = GIVEN_FIRST[(seed * 3 + 7) % len(GIVEN_FIRST)]
    second = GIVEN_SECOND[(seed * 5 + 11) % len(GIVEN_SECOND)]
    if seed % 5 == 0:
        return family_name + first
    return family_name + first + second


def make_phone(seed: int) -> str:
    prefix = PHONE_PREFIXES[seed % len(PHONE_PREFIXES)]
    suffix = (seed * 7919 + 2468) % 100000000
    return f"{prefix}{suffix:08d}"


def make_identity(seed: int, birth_date: date, male: bool = True) -> str:
    seq = 100 + (seed % 900)
    if male and seq % 2 == 0:
        seq += 1
    if not male and seq % 2 == 1:
        seq += 1
    if seq > 999:
        seq -= 2
    body = f"{AREA_CODE}{birth_date:%Y%m%d}{seq:03d}"
    checksum = CHECK_CODES[sum(int(num) * weight for num, weight in zip(body, WEIGHTS)) % 11]
    return body + checksum


def decimal_money(value: Decimal) -> Decimal:
    return value.quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)


def sql_value(value: object) -> str:
    if value is None:
        return "NULL"
    if isinstance(value, bool):
        return "1" if value else "0"
    if isinstance(value, Decimal):
        return format(value, "f")
    if isinstance(value, datetime):
        return f"'{value:%Y-%m-%d %H:%M:%S}'"
    if isinstance(value, date):
        return f"'{value:%Y-%m-%d}'"
    if isinstance(value, str):
        escaped = value.replace("\\", "\\\\").replace("'", "''")
        return f"'{escaped}'"
    return str(value)


def emit_insert(table: str, columns: Iterable[str], rows: list[tuple[object, ...]], batch_size: int = 100) -> list[str]:
    columns_sql = ", ".join(columns)
    statements: list[str] = []
    for index in range(0, len(rows), batch_size):
        batch = rows[index:index + batch_size]
        values_sql = ",\n".join(
            "    (" + ", ".join(sql_value(value) for value in row) + ")" for row in batch
        )
        statements.append(f"INSERT INTO {table} ({columns_sql}) VALUES\n{values_sql};")
    return statements


def generate_buildings() -> list[tuple[object, ...]]:
    floor_counts = [18, 18, 20, 20, 22, 22, 18, 18, 24, 24, 20, 20]
    rows = []
    for building_id in range(1, BUILDING_COUNT + 1):
        created_at = datetime(2024, 3, 1, 9, 0, 0) + timedelta(days=building_id * 4)
        rows.append(
            (
                building_id,
                f"云栖雅苑{building_id}号楼",
                f"YQ-{building_id:02d}",
                f"杭州市滨江区江南大道688号云栖雅苑{building_id}号楼",
                floor_counts[building_id - 1],
                UNITS_PER_BUILDING,
                created_at,
                created_at,
            )
        )
    return rows


def occupancy_for(unit_id: int) -> str:
    if unit_id <= 130:
        return "OCCUPIED"
    if unit_id <= NON_VACANT_UNITS:
        return "RENTED"
    return "VACANT"


def generate_units() -> list[UnitRecord]:
    units: list[UnitRecord] = []
    unit_id = 1
    area_base = [Decimal("86.00"), Decimal("89.50"), Decimal("96.00"), Decimal("102.00"), Decimal("108.50"), Decimal("118.00")]
    for building_id in range(1, BUILDING_COUNT + 1):
        for floor_no in range(1, 11):
            for room_index in range(1, 3):
                room_no = f"{floor_no}{room_index:02d}"
                area = area_base[(unit_id + room_index + building_id) % len(area_base)] + Decimal((building_id % 3) * 2)
                created_at = datetime(2024, 4, 1, 10, 0, 0) + timedelta(days=unit_id)
                units.append(
                    UnitRecord(
                        id=unit_id,
                        building_id=building_id,
                        unit_no=f"{building_id:02d}栋{room_no}",
                        floor_no=floor_no,
                        area_m2=decimal_money(area),
                        occupancy_status=occupancy_for(unit_id),
                        owner_name=make_name(1000 + unit_id),
                        owner_phone=make_phone(2000 + unit_id),
                        created_at=created_at,
                    )
                )
                unit_id += 1
    return units


def resident_birth(seed: int, min_age: int, max_age: int) -> date:
    age = min_age + (seed % (max_age - min_age + 1))
    month = (seed % 12) + 1
    day = (seed % 27) + 1
    return date(2026 - age, month, day)


def generate_residents(units: list[UnitRecord]) -> tuple[list[tuple[object, ...]], dict[int, ResidentProfile]]:
    rows: list[tuple[object, ...]] = []
    primary_profiles: dict[int, ResidentProfile] = {}
    resident_id = 1
    moved_out_second_resident_units = set(range(9, 9 + 20)) | set(range(141, 141 + 12))

    for unit in units:
        if unit.occupancy_status == "VACANT":
            continue

        move_in_primary = date(2020, 1, 5) + timedelta(days=unit.id * 11)
        if unit.occupancy_status == "OCCUPIED":
            primary_name = unit.owner_name
            primary_type = "OWNER"
            primary_birth = resident_birth(unit.id, 30, 61)
            family_name = make_name(5000 + unit.id, surname=primary_name[0])
            family_type = "FAMILY"
            family_birth = resident_birth(7000 + unit.id, 8, 35)
        else:
            primary_name = make_name(3000 + unit.id)
            primary_type = "TENANT"
            primary_birth = resident_birth(3000 + unit.id, 24, 48)
            family_name = make_name(9000 + unit.id, surname=primary_name[0])
            family_type = "FAMILY"
            family_birth = resident_birth(11000 + unit.id, 5, 42)

        primary_phone = make_phone(4000 + unit.id)
        primary_profiles[unit.id] = ResidentProfile(unit_id=unit.id, primary_name=primary_name, primary_phone=primary_phone)
        rows.append(
            (
                resident_id,
                unit.id,
                primary_name,
                primary_phone,
                make_identity(resident_id, primary_birth, male=resident_id % 2 == 1),
                primary_type,
                "ACTIVE",
                move_in_primary,
                None,
                datetime.combine(move_in_primary, time(9, 30)),
                datetime.combine(move_in_primary, time(9, 30)),
            )
        )
        resident_id += 1

        family_move_in = move_in_primary + timedelta(days=20 + (unit.id % 60))
        moved_out = unit.id in moved_out_second_resident_units
        move_out_date = family_move_in + timedelta(days=260 + (unit.id % 480)) if moved_out else None
        family_status = "MOVED_OUT" if moved_out else "ACTIVE"
        updated_at = datetime.combine(move_out_date, time(16, 0)) if moved_out else datetime.combine(family_move_in, time(10, 0))
        rows.append(
            (
                resident_id,
                unit.id,
                family_name,
                make_phone(6000 + unit.id),
                make_identity(resident_id, family_birth, male=resident_id % 2 == 1),
                family_type,
                family_status,
                family_move_in,
                move_out_date,
                datetime.combine(family_move_in, time(10, 0)),
                updated_at,
            )
        )
        resident_id += 1

    return rows, primary_profiles


def bill_month_for(unit_id: int) -> str:
    if unit_id <= 80:
        return "2026-01"
    if unit_id <= 160:
        return "2026-02"
    return "2026-03"


def generate_fee_bills(units: list[UnitRecord]) -> list[tuple[object, ...]]:
    rows: list[tuple[object, ...]] = []
    for unit in units:
        bill_id = unit.id
        billing_month = bill_month_for(unit.id)
        due_date = date.fromisoformat(f"{billing_month}-25")
        rate = Decimal("3.10") + Decimal((unit.building_id % 4)) * Decimal("0.18")
        amount = decimal_money(unit.area_m2 * rate + Decimal("58.00"))

        if bill_id <= 150:
            status = "PAID"
            paid_amount = amount
            paid_at = datetime.combine(due_date - timedelta(days=(bill_id % 6) + 1), time(15, 20))
        elif bill_id <= 195:
            status = "UNPAID"
            paid_amount = Decimal("0.00")
            paid_at = None
        elif bill_id <= 220:
            status = "PARTIAL"
            ratio = Decimal("0.45") + Decimal(bill_id % 3) * Decimal("0.10")
            paid_amount = decimal_money(amount * ratio)
            paid_at = datetime.combine(due_date - timedelta(days=2), time(11, 10))
        else:
            status = "OVERDUE"
            paid_amount = Decimal("0.00")
            paid_at = None

        remark_prefix = OWNER_NOTES[bill_id % len(OWNER_NOTES)]
        if status == "PAID":
            remarks = remark_prefix + " 本月已按时结清。"
        elif status == "PARTIAL":
            remarks = remark_prefix + " 已收到部分款项，待补缴剩余金额。"
        elif status == "OVERDUE":
            remarks = remark_prefix + " 已超过缴费期限，物业已电话提醒。"
        else:
            remarks = remark_prefix + " 账单待住户处理。"

        created_at = datetime.combine(date.fromisoformat(f"{billing_month}-01"), time(8, 30))
        updated_at = paid_at or datetime.combine(due_date + timedelta(days=7), time(9, 0))
        rows.append(
            (
                bill_id,
                f"FY{billing_month.replace('-', '')}{bill_id:04d}",
                unit.id,
                billing_month,
                amount,
                paid_amount,
                status,
                due_date,
                paid_at,
                remarks,
                created_at,
                updated_at,
            )
        )
    return rows


def generate_work_orders(units: list[UnitRecord], profiles: dict[int, ResidentProfile]) -> list[tuple[object, ...]]:
    active_units = [unit for unit in units if unit.id in profiles]
    rows: list[tuple[object, ...]] = []
    for order_id in range(1, 146):
        unit = active_units[(order_id * 7) % len(active_units)]
        resident = profiles[unit.id]
        category = WORK_ORDER_CATEGORIES[(order_id - 1) % len(WORK_ORDER_CATEGORIES)]
        priority = "HIGH" if order_id <= 25 else ("LOW" if order_id > 105 else "MEDIUM")
        if order_id <= 18:
            status = "OPEN"
        elif order_id <= 40:
            status = "IN_PROGRESS"
        elif order_id <= 110:
            status = "DONE"
        else:
            status = "CLOSED"

        created_at = datetime(2026, 1, 3, 9, 0, 0) + timedelta(days=order_id * 2, hours=order_id % 7)
        assignee = None if status == "OPEN" else ASSIGNEES[order_id % len(ASSIGNEES)]
        scheduled_at = None if status == "OPEN" else created_at + timedelta(hours=6 + (order_id % 18))
        finished_at = None if status in {"OPEN", "IN_PROGRESS"} else scheduled_at + timedelta(hours=4 + (order_id % 10))
        description = WORK_ORDER_TEMPLATES[category]
        if category == "公共区域维护":
            description += f" 位置为 {unit.unit_no} 所在楼层附近。"
        elif category == "电梯报修":
            description += f" 由 {unit.unit_no} 住户反馈。"

        updated_at = finished_at or scheduled_at or created_at
        rows.append(
            (
                order_id,
                f"GD2026{order_id:04d}",
                unit.id,
                resident.primary_name,
                resident.primary_phone,
                category,
                priority,
                description,
                status,
                assignee,
                scheduled_at,
                finished_at,
                created_at,
                updated_at,
            )
        )
    return rows


def generate_users() -> list[tuple[object, ...]]:
    return [
        (1, "admin", PASSWORD_HASHES["admin"], "", "ADMIN", "ACTIVE", datetime(2025, 6, 1, 9, 0), datetime(2025, 6, 1, 9, 0)),
        (2, "manager", PASSWORD_HASHES["manager"], "", "STAFF", "ACTIVE", datetime(2025, 6, 1, 9, 5), datetime(2025, 6, 1, 9, 5)),
        (3, "finance", PASSWORD_HASHES["finance"], "", "FINANCE", "ACTIVE", datetime(2025, 6, 1, 9, 10), datetime(2025, 6, 1, 9, 10)),
    ]


def build_sql() -> str:
    buildings = generate_buildings()
    units = generate_units()
    residents, profiles = generate_residents(units)
    bills = generate_fee_bills(units)
    orders = generate_work_orders(units, profiles)
    users = generate_users()

    total_rows = len(buildings) + len(units) + len(residents) + len(bills) + len(orders) + len(users)
    if total_rows != TARGET_TOTAL_ROWS:
        raise ValueError(f"Expected {TARGET_TOTAL_ROWS} rows, generated {total_rows} rows")

    lines = [
        "-- 物业管理系统真实中文示例数据（合成数据）",
        "-- 数据量：buildings=12, units=240, residents=360, fee_bills=240, work_orders=145, users=3，总计 1000 条",
        "SET NAMES utf8mb4;",
        "SET time_zone = '+08:00';",
        "SET FOREIGN_KEY_CHECKS = 0;",
        "TRUNCATE TABLE fee_bills;",
        "TRUNCATE TABLE work_orders;",
        "TRUNCATE TABLE residents;",
        "TRUNCATE TABLE units;",
        "TRUNCATE TABLE buildings;",
        "TRUNCATE TABLE users;",
        "SET FOREIGN_KEY_CHECKS = 1;",
        "START TRANSACTION;",
        "",
    ]

    lines.extend(emit_insert(
        "buildings",
        ["id", "name", "code", "address", "floor_count", "unit_count", "created_at", "updated_at"],
        buildings,
        batch_size=50,
    ))
    lines.append("")
    lines.extend(emit_insert(
        "units",
        ["id", "building_id", "unit_no", "floor_no", "area_m2", "occupancy_status", "owner_name", "owner_phone", "created_at", "updated_at"],
        [
            (unit.id, unit.building_id, unit.unit_no, unit.floor_no, unit.area_m2, unit.occupancy_status, unit.owner_name, unit.owner_phone, unit.created_at, unit.created_at)
            for unit in units
        ],
        batch_size=80,
    ))
    lines.append("")
    lines.extend(emit_insert(
        "residents",
        ["id", "unit_id", "name", "phone", "identity_no", "resident_type", "status", "move_in_date", "move_out_date", "created_at", "updated_at"],
        residents,
        batch_size=120,
    ))
    lines.append("")
    lines.extend(emit_insert(
        "fee_bills",
        ["id", "bill_no", "unit_id", "billing_month", "amount", "paid_amount", "status", "due_date", "paid_at", "remarks", "created_at", "updated_at"],
        bills,
        batch_size=80,
    ))
    lines.append("")
    lines.extend(emit_insert(
        "work_orders",
        ["id", "order_no", "unit_id", "resident_name", "phone", "category", "priority", "description", "status", "assignee", "scheduled_at", "finished_at", "created_at", "updated_at"],
        orders,
        batch_size=80,
    ))
    lines.append("")
    lines.extend(emit_insert(
        "users",
        ["id", "username", "password_hash", "password_salt", "role", "status", "created_at", "updated_at"],
        users,
        batch_size=20,
    ))
    lines.extend([
        "",
        "COMMIT;",
        "",
        "SELECT 'buildings' AS table_name, COUNT(*) AS row_count FROM buildings",
        "UNION ALL SELECT 'units', COUNT(*) FROM units",
        "UNION ALL SELECT 'residents', COUNT(*) FROM residents",
        "UNION ALL SELECT 'fee_bills', COUNT(*) FROM fee_bills",
        "UNION ALL SELECT 'work_orders', COUNT(*) FROM work_orders",
        "UNION ALL SELECT 'users', COUNT(*) FROM users;",
    ])
    return "\n".join(lines) + "\n"


def main() -> None:
    parser = argparse.ArgumentParser(description="Generate realistic Chinese sample SQL for the property management database.")
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT, help="Output SQL file path")
    args = parser.parse_args()

    output_path = args.output.resolve()
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(build_sql(), encoding="utf-8")
    print(f"Generated SQL: {output_path}")


if __name__ == "__main__":
    main()

