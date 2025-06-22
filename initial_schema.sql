-- 启用 pgcrypto 扩展以使用 gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. 用户核心表
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    hashed_password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('STUDENT', 'ADMIN')),
    -- [已调整] 新增字段以支持算法的硬性筛选
    gender VARCHAR(10) NOT NULL CHECK (gender IN ('MALE', 'FEMALE')),
    college VARCHAR(100) NOT NULL,
    is_special_needs BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 2. 匹配周期表
CREATE TABLE matching_cycles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    start_date TIMESTAMPTZ,
    end_date TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'OPEN', 'PROCESSING', 'COMPLETED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 3. 动态问卷维度表
CREATE TABLE survey_dimensions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cycle_id UUID NOT NULL REFERENCES matching_cycles(id) ON DELETE CASCADE,
    dimension_key VARCHAR(100) NOT NULL,
    prompt TEXT NOT NULL,
    -- [已调整] 明确维度的用途
    dimension_type VARCHAR(20) NOT NULL CHECK (dimension_type IN ('HARD_FILTER', 'SOFT_FACTOR')),
    -- [已调整] 增加复合类型
    response_type VARCHAR(20) NOT NULL CHECK (response_type IN ('SCALE', 'SINGLE_CHOICE', 'COMPOSITE')),
    weight DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    -- [已调整] 新增字段支持复合维度
    parent_dimension_key VARCHAR(100),
    -- [已调整] 新增字段支持反向计分
    is_reverse_scored BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE(cycle_id, dimension_key)
);

-- 4. 维度选项表
CREATE TABLE dimension_options (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dimension_id UUID NOT NULL REFERENCES survey_dimensions(id) ON DELETE CASCADE,
    option_text VARCHAR(255) NOT NULL,
    option_value DOUBLE PRECISION NOT NULL
);

-- 5. 学生答案表
CREATE TABLE user_responses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    dimension_id UUID NOT NULL REFERENCES survey_dimensions(id) ON DELETE CASCADE,
    -- [已调整] 存储原始值，预处理在后端进行
    raw_value DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(user_id, dimension_id)
);

-- 6. 宿舍资源 - 楼栋
CREATE TABLE dorm_buildings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE
);

-- 7. 宿舍资源 - 房间
CREATE TABLE dorm_rooms (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    building_id UUID NOT NULL REFERENCES dorm_buildings(id) ON DELETE CASCADE,
    room_number VARCHAR(20) NOT NULL,
    capacity INT NOT NULL,
    -- [已调整] 与 users 表的 gender 严格对应
    gender_type VARCHAR(20) NOT NULL CHECK (gender_type IN ('MALE', 'FEMALE')),
    UNIQUE(building_id, room_number)
);

-- 8. 宿舍资源 - 床位
CREATE TABLE beds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id UUID NOT NULL REFERENCES dorm_rooms(id) ON DELETE CASCADE,
    bed_number INT NOT NULL,
    UNIQUE(room_id, bed_number)
);

-- 9. 最终匹配结果表
CREATE TABLE matching_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cycle_id UUID NOT NULL REFERENCES matching_cycles(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE UNIQUE,
    bed_id UUID NOT NULL REFERENCES beds(id) ON DELETE CASCADE UNIQUE,
    match_group_id UUID NOT NULL
);

-- 10. 入住后反馈表
CREATE TABLE feedback (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cycle_id UUID NOT NULL REFERENCES matching_cycles(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    is_anonymous BOOLEAN NOT NULL DEFAULT FALSE,
    rating INT CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 11. 调宿申请表
CREATE TABLE swap_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    cycle_id UUID NOT NULL REFERENCES matching_cycles(id) ON DELETE CASCADE,
    reason TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    admin_comment TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 12. 自动化通知表
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    message TEXT NOT NULL,
    link_url VARCHAR(255),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 13. 内容发布表 (心理健康/宿舍文化)
CREATE TABLE content_articles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    category VARCHAR(50) NOT NULL,
    author_id UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
