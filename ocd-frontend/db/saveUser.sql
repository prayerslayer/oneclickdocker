WITH user_update AS (
     UPDATE users
        SET u_id = $1,
            u_name = $2,
            u_email = $3,
            u_display_name = $4,
            u_plan = COALESCE($5, u_plan),
            u_plan_expires = COALESCE($6, u_plan_expires)
      WHERE u_id = $1
  RETURNING *)
INSERT INTO users (
            u_id,
            u_name,
            u_email,
            u_display_name)
     SELECT $1,
            $2,
            $3,
            $4
      WHERE NOT EXISTS (SELECT * FROM user_update);
