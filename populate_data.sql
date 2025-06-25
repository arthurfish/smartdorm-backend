-- (可选) 在执行前清空所有相关表的数据。
-- 在开发环境中非常有用，可以重复执行此脚本来重置数据。
-- TRUNCATE TABLE users, matching_cycles, survey_dimensions, dimension_options, user_responses, dorm_buildings, dorm_rooms, beds, matching_results, feedback, swap_requests, notifications, content_articles RESTART IDENTITY CASCADE;

-- 使用匿名代码块来声明变量并执行逻辑
DO $$
    DECLARE
        -- 声明变量以存储动态生成的UUID
        admin_user_id UUID;

        -- 匹配周期 IDs
        completed_cycle_id UUID;
        open_cycle_id UUID;
        draft_cycle_id UUID;

        -- 宿舍楼 IDs
        male_building_id UUID;
        female_building_id UUID;

        -- 问卷维度 IDs (用于开放周期)
        dim_atmosphere_id UUID;
        dim_wake_time_id UUID;
        dim_sleep_time_id UUID;
        dim_clean_freq_id UUID;

        -- 大五人格父维度 IDs
        dim_neuroticism_id UUID;
        dim_extraversion_id UUID;
        dim_openness_id UUID;
        dim_agreeableness_id UUID;
        dim_conscientiousness_id UUID;

        -- 用于循环和临时存储的辅助变量
        student_id_counter INT;
        temp_user_id UUID;
        temp_room_id UUID;

        -- 用于演示匹配结果的组ID
        male_match_group_id UUID := gen_random_uuid();
        female_match_group_id UUID := gen_random_uuid();

        -- 用于生成随机数据的数组
        colleges TEXT[] := ARRAY['计算机科学与技术学院', '物理学院', '文学院', '数学科学学院', '经济管理学院'];
        male_first_names TEXT[] := ARRAY['伟', '磊', '强', '宇', '洋', '鹏', '杰', '明', '昊', '鑫'];
        female_first_names TEXT[] := ARRAY['芳', '敏', '静', '秀', '琳', '娜', '婷', '慧', '丹', '薇'];
        last_names TEXT[] := ARRAY['李', '王', '张', '刘', '陈', '杨', '黄', '赵', '吴', '周'];

    BEGIN

        -- === 1. USERS ===
-- 创建管理员用户 (密码: password) 并捕获其UUID
        INSERT INTO users (student_id, name, hashed_password, role, gender, college, is_special_needs)
        VALUES ('admin', '系统管理员', '$2a$10$ki8rnyxLO.tWr8BUlv6VoeLmgEzQ10Zt.2GS39354wZ3rLAjvg7dS'
        , 'ADMIN', 'MALE', '信息中心', FALSE)
        RETURNING id INTO admin_user_id;

        -- 循环创建学生用户 (密码: password)
-- 40 Male Students
        FOR i IN 1..40 LOOP
                student_id_counter := 202401000 + i;
                INSERT INTO users (student_id, name, hashed_password, role, gender, college)
                VALUES (
                           student_id_counter::TEXT,
                           last_names[floor(random() * 10 + 1)] || male_first_names[floor(random() * 10 + 1)],
                           '$2a$10$ki8rnyxLO.tWr8BUlv6VoeLmgEzQ10Zt.2GS39354wZ3rLAjvg7dS', -- password
                           'STUDENT', 'MALE', colleges[floor(random() * 5 + 1)]
                       );
            END LOOP;

-- 40 Female Students
        FOR i IN 1..40 LOOP
                student_id_counter := 202402000 + i;
                INSERT INTO users (student_id, name, hashed_password, role, gender, college)
                VALUES (
                           student_id_counter::TEXT,
                           last_names[floor(random() * 10 + 1)] || female_first_names[floor(random() * 10 + 1)],
                           '$2a$10$9.Mv4V0/9a823QlGpB.syeiY1s.8VfI1FBC21u5gEOY25rG1dIe3O', -- password
                           'STUDENT', 'FEMALE', colleges[floor(random() * 5 + 1)]
                       );
            END LOOP;


        -- === 2. MATCHING CYCLES ===
-- 创建周期并捕获UUIDs
        INSERT INTO matching_cycles (name, start_date, end_date, status)
        VALUES ('2023级秋季新生分配', '2023-08-01 00:00:00+08', '2023-08-15 23:59:59+08', 'COMPLETED')
        RETURNING id INTO completed_cycle_id;

        INSERT INTO matching_cycles (name, start_date, end_date, status)
        VALUES ('2024级秋季新生分配', '2024-08-01 00:00:00+08', '2024-08-20 23:59:59+08', 'OPEN')
        RETURNING id INTO open_cycle_id;

        INSERT INTO matching_cycles (name, status)
        VALUES ('2025年春季学期调宿', 'DRAFT')
        RETURNING id INTO draft_cycle_id;


        -- === 3. DORM RESOURCES (Buildings, Rooms, Beds) ===
-- 创建楼栋并捕获UUIDs
        INSERT INTO dorm_buildings (name) VALUES ('紫荆1号楼') RETURNING id INTO male_building_id;
        INSERT INTO dorm_buildings (name) VALUES ('紫荆2号楼') RETURNING id INTO female_building_id;

-- 创建10个男生房间，每个房间4个床位
        FOR i IN 1..10 LOOP
                INSERT INTO dorm_rooms (building_id, room_number, capacity, gender_type)
                VALUES (male_building_id, (100+i)::TEXT, 4, 'MALE')
                RETURNING id INTO temp_room_id;

                INSERT INTO beds (room_id, bed_number) SELECT temp_room_id, j FROM generate_series(1, 4) j;
            END LOOP;

-- 创建10个女生房间，每个房间4个床位
        FOR i IN 1..10 LOOP
                INSERT INTO dorm_rooms (building_id, room_number, capacity, gender_type)
                VALUES (female_building_id, (200+i)::TEXT, 4, 'FEMALE')
                RETURNING id INTO temp_room_id;

                INSERT INTO beds (room_id, bed_number) SELECT temp_room_id, j FROM generate_series(1, 4) j;
            END LOOP;


        -- === 4. SURVEY DIMENSIONS & OPTIONS (For the 'OPEN' cycle) ===
-- 为 '2024级秋季新生分配' 周期创建问卷

-- 4.1 硬性筛选维度
        INSERT INTO survey_dimensions(cycle_id, dimension_key, prompt, dimension_type, response_type, weight)
        VALUES (open_cycle_id, 'atmosphere', '你喜欢的宿舍氛围是？', 'HARD_FILTER', 'SINGLE_CHOICE', 1.0)
        RETURNING id INTO dim_atmosphere_id;

        INSERT INTO dimension_options(dimension_id, option_text, option_value)
        VALUES (dim_atmosphere_id, '安静', 1.0), (dim_atmosphere_id, '热闹', 2.0);

-- 4.2 软性匹配因子
        INSERT INTO survey_dimensions(cycle_id, dimension_key, prompt, dimension_type, response_type, weight)
        VALUES (open_cycle_id, 'wake_time', '你的起床时间一般是？', 'SOFT_FACTOR', 'SINGLE_CHOICE', 0.4) RETURNING id INTO dim_wake_time_id;
        INSERT INTO dimension_options(dimension_id, option_text, option_value)
        VALUES (dim_wake_time_id, '7点以前', 1.0), (dim_wake_time_id, '7点到8点', 2.0), (dim_wake_time_id, '8点到9点', 3.0), (dim_wake_time_id, '9点以后', 4.0);

        INSERT INTO survey_dimensions(cycle_id, dimension_key, prompt, dimension_type, response_type, weight)
        VALUES (open_cycle_id, 'sleep_time', '你的入睡时间一般是？', 'SOFT_FACTOR', 'SINGLE_CHOICE', 0.4) RETURNING id INTO dim_sleep_time_id;
        INSERT INTO dimension_options(dimension_id, option_text, option_value)
        VALUES (dim_sleep_time_id, '22点前', 1.0), (dim_sleep_time_id, '22点到23点', 2.0), (dim_sleep_time_id, '23点到0点', 3.0), (dim_sleep_time_id, '0点后', 4.0);

        INSERT INTO survey_dimensions(cycle_id, dimension_key, prompt, dimension_type, response_type, weight)
        VALUES (open_cycle_id, 'clean_freq', '你认为打扫宿舍适宜的频率是？', 'SOFT_FACTOR', 'SINGLE_CHOICE', 0.3) RETURNING id INTO dim_clean_freq_id;
        INSERT INTO dimension_options(dimension_id, option_text, option_value)
        VALUES (dim_clean_freq_id, '每天打扫', 1.0), (dim_clean_freq_id, '每周2到3次', 2.0), (dim_clean_freq_id, '每周1次', 3.0), (dim_clean_freq_id, '几乎不打扫', 4.0);

-- 4.3 大五人格复合父维度
        INSERT INTO survey_dimensions(cycle_id, dimension_key, prompt, dimension_type, response_type, weight)
        VALUES
            (open_cycle_id, 'neuroticism', '神经质（情绪稳定性）', 'SOFT_FACTOR', 'COMPOSITE', 0.3),
            (open_cycle_id, 'extraversion', '外向性（社交活跃度）', 'SOFT_FACTOR', 'COMPOSITE', 0.2),
            (open_cycle_id, 'openness', '开放性（思维开放性）', 'SOFT_FACTOR', 'COMPOSITE', 0.2),
            (open_cycle_id, 'agreeableness', '宜人性（合作性）', 'SOFT_FACTOR', 'COMPOSITE', 0.3),
            (open_cycle_id, 'conscientiousness', '谨慎性（自律性）', 'SOFT_FACTOR', 'COMPOSITE', 0.2);

-- 4.4 大五人格量表子问题
        INSERT INTO survey_dimensions(cycle_id, dimension_key, prompt, dimension_type, response_type, weight, parent_dimension_key, reverse_scored)
        VALUES
            (open_cycle_id, 'n_q1', '我经常感到紧张或焦虑。', 'SOFT_FACTOR', 'SCALE', 1.0, 'neuroticism', FALSE),
            (open_cycle_id, 'n_q4', '我很少感到沮丧。', 'SOFT_FACTOR', 'SCALE', 1.0, 'neuroticism', TRUE),
            (open_cycle_id, 'e_q1', '我喜欢与人交谈并成为焦点。', 'SOFT_FACTOR', 'SCALE', 1.0, 'extraversion', FALSE),
            (open_cycle_id, 'e_q3', '独处时我会感到无聊。', 'SOFT_FACTOR', 'SCALE', 1.0, 'extraversion', TRUE),
            (open_cycle_id, 'o_q1', '我对艺术和诗歌很感兴趣。', 'SOFT_FACTOR', 'SCALE', 1.0, 'openness', FALSE),
            (open_cycle_id, 'o_q3', '我对抽象理论讨论感到无聊。', 'SOFT_FACTOR', 'SCALE', 1.0, 'openness', TRUE),
            (open_cycle_id, 'a_q1', '我通常会原谅别人的过错。', 'SOFT_FACTOR', 'SCALE', 1.0, 'agreeableness', FALSE),
            (open_cycle_id, 'a_q2', '我乐意帮助陌生人。', 'SOFT_FACTOR', 'SCALE', 1.0, 'agreeableness', FALSE),
            (open_cycle_id, 'c_q1', '我会提前规划好每天的任务。', 'SOFT_FACTOR', 'SCALE', 1.0, 'conscientiousness', FALSE),
            (open_cycle_id, 'c_q3', '我经常拖延作业。', 'SOFT_FACTOR', 'SCALE', 1.0, 'conscientiousness', TRUE);
        -- ... (为简洁起见，此处仅列出部分正向和反向计分题，您可以按需补全所有20道题) ...


-- === 5. USER RESPONSES (For the 'OPEN' cycle) ===
-- 为所有学生和开放周期中的问卷问题批量生成随机答案
        INSERT INTO user_responses(user_id, dimension_id, raw_value)
        SELECT
            u.id as user_id,
            d.id as dimension_id,
            CASE
                WHEN d.response_type = 'SCALE' THEN floor(random() * 5 + 1) -- 1-5分
                WHEN d.dimension_key = 'atmosphere' THEN floor(random() * 2 + 1) -- 1-2分
                ELSE floor(random() * 4 + 1) -- 其他单选1-4分
                END as raw_value
        FROM
            users u
                CROSS JOIN
            survey_dimensions d
        WHERE
            u.role = 'STUDENT'
          AND d.cycle_id = open_cycle_id
          AND d.response_type != 'COMPOSITE'; -- 排除父维度本身


-- === 6. MATCHING RESULTS (For the 'COMPLETED' cycle) ===
-- 为已完成的周期生成一些示例匹配结果
        WITH ranked_male_students AS (
            SELECT id, row_number() OVER (ORDER BY student_id) as rn FROM users WHERE gender = 'MALE' LIMIT 4
        ),
             ranked_female_students AS (
                 SELECT id, row_number() OVER (ORDER BY student_id) as rn FROM users WHERE gender = 'FEMALE' LIMIT 4
             ),
             ranked_male_beds AS (
                 SELECT id, row_number() OVER (ORDER BY id) as rn FROM beds WHERE room_id IN (SELECT id FROM dorm_rooms WHERE gender_type = 'MALE') LIMIT 4
             ),
             ranked_female_beds AS (
                 SELECT id, row_number() OVER (ORDER BY id) as rn FROM beds WHERE room_id IN (SELECT id FROM dorm_rooms WHERE gender_type = 'FEMALE') LIMIT 4
             )
        INSERT INTO matching_results (cycle_id, user_id, bed_id, match_group_id)
        SELECT completed_cycle_id, s.id, b.id, male_match_group_id
        FROM ranked_male_students s JOIN ranked_male_beds b ON s.rn = b.rn
        UNION ALL
        SELECT completed_cycle_id, s.id, b.id, female_match_group_id
        FROM ranked_female_students s JOIN ranked_female_beds b ON s.rn = b.rn;


-- === 7. CONTENT ARTICLES ===
        INSERT INTO content_articles (title, content, category, author_id)
        VALUES
            ('有效沟通：如何与室友建立良好关系', '第一步是学会倾听。当室友向你表达观点时，请放下手中的事情，认真聆听...', '心理健康', admin_user_id),
            ('宿舍清洁挑战赛活动方案', '为了营造一个干净整洁的居住环境，学生会决定举办第一届“宿舍清洁挑战赛”...', '宿舍文化', admin_user_id);


        -- === 8. NOTIFICATIONS, FEEDBACK & SWAP REQUESTS (For completed cycle) ===
-- 选取一名有分配结果的学生来创建后续记录
        SELECT user_id INTO temp_user_id FROM matching_results WHERE match_group_id = male_match_group_id LIMIT 1;

        IF FOUND THEN
            -- 创建通知
            INSERT INTO notifications (user_id, message, link_url, is_read)
            VALUES (temp_user_id, '您在“2023级秋季新生分配”中的宿舍分配结果已公布！', '/student/result', FALSE);

            -- 创建反馈
            INSERT INTO feedback (cycle_id, user_id, is_anonymous, rating, comment)
            VALUES (completed_cycle_id, temp_user_id, FALSE, 5, '室友们都非常棒，相处很愉快，感谢系统的分配！');
        END IF;

-- 为另一名学生创建调宿申请
        SELECT user_id INTO temp_user_id FROM matching_results WHERE match_group_id = female_match_group_id LIMIT 1;

        IF FOUND THEN
            INSERT INTO swap_requests (user_id, cycle_id, reason, status)
            VALUES (temp_user_id, completed_cycle_id, '因个人作息与同寝室其他同学差异过大，严重影响睡眠质量，希望能调整至早睡早起的寝室。', 'PENDING');
        END IF;

    END $$;